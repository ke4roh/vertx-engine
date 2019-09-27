package com.redhat.vertx.pipeline;

import java.util.*;
import java.util.function.Function;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import com.redhat.vertx.Engine;

import io.reactivex.*;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.core.eventbus.EventBus;
import org.kohsuke.MetaInfServices;

@MetaInfServices(Step.class)
public class Section implements Step {
    private static final Logger logger = Logger.getLogger(Section.class.getName());
    private static List<String> RESERVED_WORDS =
            Arrays.asList("name", "register", "steps", "timeout", "concurrent", "return", "when");

    private Engine engine;
    private String name;
    private List<StepExecutor> steps;
    private Function<Iterable<? extends MaybeSource<Object>>, Flowable<Object>> comboTechnique;

    public Section() {

    }

    private Step buildStep(JsonObject def) {
        ServiceLoader<Step> serviceLoader = ServiceLoader.load(Step.class);
        final Optional<ServiceLoader.Provider<Step>> stepClass;

        var allShortNames = serviceLoader.stream()
                .collect(Collectors.toMap(
                        provider -> provider.get().getShortName(),
                        Optional::of));

        // Get all the config keys, strip out reserved words
        final var defKeys = new HashSet<>(def.getMap().keySet());
        defKeys.removeAll(RESERVED_WORDS);

        // We had more than the short name of the step, error
        if (defKeys.size() > 1) {
            throw new RuntimeException("Unknown keys in configuration: " + defKeys.toString());
        }

        // We should only have one entry, use that for the short name to class mapping
        stepClass = allShortNames.getOrDefault(defKeys.toArray()[0].toString(), Optional.empty());

        // Return the class found or error
        if (stepClass.isPresent()) {
            return stepClass.get().get();
        } else {
            throw new RuntimeException("Error locating step implementation");
        }
    }

    public String getName() {
        return name;
    }

    @Override
    public Completable init(Engine engine, JsonObject config) {
        this.engine = engine;
        this.name = config.getString("name","default");
        List<Completable> stepInitCompletables = new ArrayList<>();
        List<StepExecutor> steps = new ArrayList<>();
        comboTechnique = config.getBoolean("concurrent",false) ?
                Maybe::mergeDelayError : Maybe::concat;

        JsonArray innerSteps = config.getJsonObject(getShortName(), config).getJsonArray("steps", new JsonArray());
        innerSteps.forEach( stepConfig -> {
            Step s = buildStep((JsonObject)stepConfig);
            stepInitCompletables.add(s.init(engine,(JsonObject)stepConfig));
            steps.add(new StepExecutor(engine,s,(JsonObject)stepConfig));
        });
        this.steps=Collections.unmodifiableList(steps);
        return Completable.merge(stepInitCompletables);
    }

    public Maybe<Object> execute(JsonObject environment) {
        String documentId = environment.getJsonObject("doc").getString(Engine.DOC_UUID);
        return comboTechnique.apply(steps.stream().map(s -> s.executeStep(documentId)).collect(Collectors.toList()))
                .doOnSubscribe(s -> publishSectionEvent(documentId, EventBusMessage.SECTION_STARTED))
                .doOnComplete(() -> publishSectionEvent(documentId, EventBusMessage.SECTION_COMPLETED))
                .doOnError(t -> publishSectionEvent(documentId, EventBusMessage.SECTION_ERRORED))
                .lastElement()
                .filter(r -> environment.getJsonObject("stepdef").getString("return",null) != null)
                .map(r -> environment.getJsonObject("stepdef").getString("return"));
    }

    private void publishSectionEvent(String documentId, String message) {
        logger.finest("Section " + message);
        EventBus bus = engine.getEventBus();
        DeliveryOptions documentIdHeader = new DeliveryOptions().addHeader("uuid", documentId);
        bus.publish(message, name, documentIdHeader);
    }
}

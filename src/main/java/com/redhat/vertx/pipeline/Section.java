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
    private static List<String> RESERVED_WORDS = Arrays.asList("name", "vars", "register", "steps", "class", "timeout", "concurrent");

    private Engine engine;
    private String name;
    private List<Step> steps;
    private JsonObject stepConfig;
    private Function<Iterable<? extends MaybeSource<JsonObject>>, Flowable<JsonObject>> comboTechnique;

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

        // We should only have one entry, use that for the sort name to class mapping
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
        List<Completable> stepCompletables = new ArrayList<>();
        List<Step> steps = new ArrayList<>();
        this.stepConfig = config.getJsonObject(getShortName(), config);
        comboTechnique = config.getBoolean("concurrent",false)?Maybe::mergeDelayError:Maybe::concat;


        stepConfig.getJsonArray("steps", new JsonArray()).forEach( stepConfig -> {
            Step s = buildStep((JsonObject)stepConfig);
            stepCompletables.add(s.init(engine,(JsonObject)stepConfig));
            steps.add(s);
        });
        this.steps=Collections.unmodifiableList(steps);
        return Completable.merge(stepCompletables);
    }

    public Maybe<JsonObject> execute(String documentId) {
        return comboTechnique.apply(steps.stream().map(s -> s.execute(documentId)).collect(Collectors.toList()))
                .flatMapCompletable(r -> engine.updateDocument(documentId, r))
                .doOnSubscribe(s -> publishSectionEvent(documentId, EventBusMessage.SECTION_STARTED))
                .doOnComplete(() -> publishSectionEvent(documentId, EventBusMessage.SECTION_COMPLETED))
                .doOnError(t -> publishSectionEvent(documentId, EventBusMessage.SECTION_ERRORED))
                .toMaybe();
    }

    @Override
    public JsonObject getConfig() {
        return this.stepConfig;
    }

    @Override
    public JsonObject getVars() {
        return this.stepConfig;
    }

    private void publishSectionEvent(String documentId, String message) {
        logger.finest("Section " + message);
        EventBus bus = engine.getEventBus();
        DeliveryOptions documentIdHeader = new DeliveryOptions().addHeader("uuid", documentId);
        bus.publish(message, name, documentIdHeader);
    }
}

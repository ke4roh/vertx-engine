package com.redhat.vertx.pipeline;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Duration;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.logging.Handler;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.redhat.ResourceUtils;
import com.redhat.vertx.DocumentLogger;
import com.redhat.vertx.Engine;
import com.redhat.vertx.pipeline.json.YamlParser;
import com.redhat.vertx.pipeline.templates.MissingParameterException;
import io.reactivex.Completable;
import io.reactivex.Maybe;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.reactivex.MaybeHelper;
import io.vertx.reactivex.core.Vertx;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.kohsuke.MetaInfServices;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.fail;

@ExtendWith(VertxExtension.class)
public class PipelineIntegrationTest {

    @MetaInfServices(Step.class)
    public static class FNFE extends AbstractStep {
        @Override
        public Maybe<Object> execute(JsonObject env) {
            return Maybe.error(new FileNotFoundException());
        }
    }

    @MetaInfServices(Step.class)
    public static class Sleep extends AbstractStep {
        static Logger logger = Logger.getLogger(Sleep.class.getName());

        @Override
        public Maybe<Object> execute(JsonObject env) {
            Duration duration = Duration.parse(env.getString("duration"));
            return Completable.timer(duration.toMillis(),TimeUnit.MILLISECONDS)
                    .toSingleDefault((Object)"Ding!")
                    .toMaybe();
        }
    }

    @Test
    public void testIncompletePipeline(Vertx vertx, VertxTestContext testContext) throws Exception {
        Engine e = new Engine(ResourceUtils.fileContentsFromResource("com/redhat/vertx/pipeline/incomplete-pipeline-test.yaml"));
        vertx.rxDeployVerticle(e).blockingGet();
        JsonObject inputDoc = new JsonObject().put("q","foo");
        assertThatExceptionOfType(MissingParameterException.class).
                isThrownBy(() -> e.execute(inputDoc).timeout(1000, TimeUnit.MILLISECONDS).blockingGet())
                .withMessage("doc.absent");
        testContext.completeNow();
    }

    @Test
    public void exceptionInStep(Vertx vertx, VertxTestContext testContext) throws Exception {
        Engine e = new Engine(ResourceUtils.fileContentsFromResource("com/redhat/vertx/pipeline/exception-in-step-pipeline-test.yaml"));
        vertx.rxDeployVerticle(e).blockingGet();
        JsonObject inputDoc = new JsonObject();
        e.execute(inputDoc).timeout(1500, TimeUnit.MILLISECONDS)
                .subscribe(doc -> testContext.failNow(new AssertionError("Exception expected")),
                        ex -> { assertThat(ex).isInstanceOf(FileNotFoundException.class); testContext.completeNow(); });
    }

    @Test
    public void testTimingForPromptStepExecution(Vertx vertx, VertxTestContext testContext) throws Exception {
        LogCapturer logCapturer = new LogCapturer(DocumentLogger.class.getName());
        logCapturer.attachLogCapturer();

        // Watch to see that the correct fields are set in the correct order
        String expectedSequence = "Lorem ipsum dolor sit Completed";
        Pattern fieldSetPattern = Pattern.compile("(?:(?:Document [0-9a-f\\-]+ field (\\w+) set)|(?:(Completed) document))");

        Engine e = new Engine(ResourceUtils.fileContentsFromResource("com/redhat/vertx/pipeline/execute-prompt-really-is-prompt.yaml"));
        vertx.rxDeployVerticle(e).blockingGet();
        JsonObject inputDoc = new JsonObject();
        // TODO make it faster (the sleep is only 1 sec, this should take only slightly longer - for parsing)
        e.execute(inputDoc).timeout(2, TimeUnit.SECONDS).blockingGet();
        Arrays.asList(Logger.getGlobal().getHandlers()).forEach(Handler::flush);
        Thread.sleep(100); // wait for the log to finish writing
        String log = logCapturer.getTestCapturedLog();
        String actualSequence = Arrays.stream(log.split("\n")).map(msg -> {
            Matcher matcher = fieldSetPattern.matcher(msg);
            if (matcher.find()) {
                String m1 = matcher.group(1);
                return (m1 == null) ? matcher.group(2) : m1;
            } else {
                return null;
            }
        }).filter(Objects::nonNull).collect(Collectors.joining(" "));
        assertThat(actualSequence).isEqualTo(expectedSequence);
        testContext.completeNow();
    }

    @Test
    public void testEmptySection(Vertx vertx, VertxTestContext testContext) throws Exception {
        Engine e = new Engine("---\n[]");
        vertx.rxDeployVerticle(e).blockingGet();
        JsonObject inputDoc = new JsonObject();
        JsonObject doc = (JsonObject) e.execute(inputDoc).timeout(500, TimeUnit.MILLISECONDS).blockingGet();

        assertThat(doc.size()).isEqualTo(1);
        assertThat(doc.containsKey(Engine.DOC_UUID)).isTrue();
        testContext.completeNow();
    }

    @Test
    public void returnASpecificValue(Vertx vertx, VertxTestContext testContext) throws Exception {
        Engine e = new Engine(ResourceUtils.fileContentsFromResource("com/redhat/vertx/pipeline/return-a-specific-value-pipeline.yaml"));
        vertx.rxDeployVerticle(e).blockingGet();
        JsonObject inputDoc = new JsonObject();
        Object result = e.execute(inputDoc).timeout(1, TimeUnit.SECONDS).blockingGet();
        assertThat(result).isInstanceOf(String.class);
        assertThat(result.toString().startsWith("<html>")).isTrue();
        testContext.completeNow();
    }

    @Test
    public void testConditionalExecution(Vertx vertx, VertxTestContext testContext) throws Exception {
        Engine e = new Engine(ResourceUtils.fileContentsFromResource("com/redhat/vertx/pipeline/test-conditional-execution.yaml"));
        vertx.rxDeployVerticle(e).blockingGet();
        JsonObject inputDoc = new JsonObject();
        Object result = e.execute(inputDoc).timeout(1, TimeUnit.SECONDS).blockingGet();
        assertThat(result).isInstanceOf(JsonObject.class);
        JsonObject doc = (JsonObject)result;
        assertThat(doc.containsKey("bat")).isTrue();
        assertThat(doc.containsKey("ball")).isFalse();
        testContext.completeNow();
    }
}

package com.redhat.vertx.pipeline;

import java.io.FileNotFoundException;
import java.time.Duration;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.redhat.ResourceUtils;
import com.redhat.vertx.DocumentLogger;
import com.redhat.vertx.Engine;
import io.reactivex.Maybe;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.reactivex.MaybeHelper;
import io.vertx.reactivex.core.Vertx;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.kohsuke.MetaInfServices;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(VertxExtension.class)
public class PipelineIntegrationTest {

    @MetaInfServices(Step.class)
    public static class FNFE extends AbstractStep {
        @Override
        protected Maybe<Object> executeSlow(JsonObject env) {
            return Maybe.error(new FileNotFoundException());
        }
    }

    @MetaInfServices(Step.class)
    public static class Sleep extends AbstractStep {
        @Override
        protected Maybe<Object> executeSlow(JsonObject env) {
            Duration duration = Duration.parse(env.getString("duration"));
            return MaybeHelper.toMaybe(handler -> {
                vertx.executeBlocking(fut -> fut.complete(sleep(duration)), handler);
            });
        }

        private Object sleep(Duration duration) {
            try {
                Thread.sleep(duration.toMillis());
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            return "Slept " + duration.toString();
        }
    }

    @Test
    public void testIncompletePipeline(Vertx vertx, VertxTestContext testContext) throws Exception {
        Engine e = new Engine(ResourceUtils.fileContentsFromResource("com/redhat/vertx/pipeline/incomplete-pipeline-test.yaml"));
        vertx.rxDeployVerticle(e).blockingGet();
        JsonObject inputDoc = new JsonObject().put("q","foo");
        JsonObject newDoc = e.execute(inputDoc).timeout(1500, TimeUnit.MILLISECONDS).blockingGet();
        assertThat(newDoc.getString("q")).isEqualTo("foo");
        assertThat(newDoc.getString("r")).isEqualTo("foo");
        assertThat(newDoc.containsKey("absent")).isFalse();
        assertThat(newDoc.containsKey("x")).isFalse();
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
        Logger log = Logger.getLogger(DocumentLogger.class.getName());
        LogCapturer logCapturer = new LogCapturer(log);

        // Watch to see that the correct fields are set in the correct order
        Iterator<String> expected = Arrays.asList("Lorem ipsum dolor sit".split(" ")).iterator();
        Pattern fieldSetPattern = Pattern.compile("Document [0-9a-f\\-]+ field (\\w+) set.");
        logCapturer.attachLogCapturer().map(msg -> {
            Matcher matcher = fieldSetPattern.matcher(msg);
            if (matcher.find()) {
                return matcher.group(1);
            } else {
                return null;
            }
        })
                .filter(Objects::nonNull)
                .subscribe( field -> assertThat(field).isEqualTo(expected.next()));

        Engine e = new Engine(ResourceUtils.fileContentsFromResource("com/redhat/vertx/pipeline/execute-prompt-really-is-prompt.yaml"));
        vertx.rxDeployVerticle(e).blockingGet();
        JsonObject inputDoc = new JsonObject();
        e.execute(inputDoc).timeout(1500, TimeUnit.MILLISECONDS).blockingGet();
        testContext.completeNow(); // TODO This is probably premature since the log will have to finish propagating
    }
}

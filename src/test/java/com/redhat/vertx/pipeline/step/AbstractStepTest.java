package com.redhat.vertx.pipeline.step;

import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import com.redhat.ResourceUtils;
import com.redhat.vertx.Engine;
import com.redhat.vertx.pipeline.AbstractStep;
import com.redhat.vertx.pipeline.Step;
import com.redhat.vertx.pipeline.templates.MissingParameterException;
import io.reactivex.Maybe;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.reactivex.core.Vertx;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.kohsuke.MetaInfServices;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(VertxExtension.class)
public class AbstractStepTest {

    @MetaInfServices(Step.class)
    public static class Concat extends AbstractStep {
        Logger logger = Logger.getLogger(this.getClass().getName());

        @Override
        public Object executeFast(JsonObject doc) {
            String base = doc.getString("from");
            if (base==null) {
                throw new MissingParameterException();
            }
            logger.finest(() -> "Step " + name + " executing.");
            return base + doc.getString("append");
        }
    }

    @Test
    public void testSequencingStepsWithIncompleteEnvironments(Vertx vertx, VertxTestContext testContext) throws Exception {
        Engine e = new Engine(ResourceUtils.fileContentsFromResource("abstract-step-test-pipeline.json"));
        vertx.rxDeployVerticle(e).blockingGet();
        JsonObject inputDoc = new JsonObject().put("x","");
        JsonObject newDoc = (JsonObject)e.execute(inputDoc).timeout(1, TimeUnit.SECONDS).blockingGet();
        assertThat(newDoc.getString("c")).isEqualTo("m");
        assertThat(newDoc.getString("ca")).isEqualTo("me");
        assertThat(newDoc.getString("cat")).isEqualTo("meo");
        assertThat(newDoc.getString("cats")).isEqualTo("meow");
        testContext.completeNow();
    }

    private void needExceptionFinishTest(Object o, String message, VertxTestContext testContext, Class<? extends Throwable> expectedException) {
        assertThat(o).isInstanceOf(expectedException);
        if (message != null) {
            assertThat(((Exception) o).getMessage()).isEqualTo(message);
        }
        testContext.completeNow();
    }

    @Test
    public void testStepAsPartOfEnvironment(Vertx vertx, VertxTestContext testContext) {
        Engine e = new Engine(ResourceUtils.fileContentsFromResource("com/redhat/vertx/pipeline/step/step-part-of-env.yml"));
        vertx.rxDeployVerticle(e).blockingGet();
        JsonObject inputDoc = new JsonObject().put("x","");
        JsonObject newDoc = (JsonObject) e.execute(inputDoc).timeout(1, TimeUnit.SECONDS).blockingGet();
        assertThat(newDoc.getString("hello")).isEqualTo("Hello from the \"step name check\" step");
        testContext.completeNow();
    }
}

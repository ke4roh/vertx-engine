package com.redhat.vertx.pipeline.step;

import com.redhat.ResourceUtils;
import com.redhat.vertx.Engine;
import com.redhat.vertx.pipeline.AbstractStep;
import com.redhat.vertx.pipeline.StepDependencyNotMetException;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.reactivex.core.Vertx;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(VertxExtension.class)
public class AbstractStepTest {

    public static class Concat extends AbstractStep {
        private String append;
        private String from;

        @Override
        public void init(Engine engine, JsonObject config) {
            super.init(engine, config);
            append = vars.getString("append");
            from = vars.getString("from");
        }

        @Override
        public Object execute(JsonObject doc) throws StepDependencyNotMetException {
            if (!doc.containsKey(from)) {
                throw new StepDependencyNotMetException(from);
            }
            String base = doc.getString(from);
            return base + append;
        }
    }

    @Test
    public void testSequencingStepsWithIncompleteEnvironments(Vertx vertx, VertxTestContext testContext) throws Exception {
        Engine e = new Engine(ResourceUtils.fileContentsFromResource("abstract-step-test-pipeline.json"));
        vertx.rxDeployVerticle(e).blockingGet();
        JsonObject inputDoc = new JsonObject().put("x","");
        JsonObject newDoc = e.execute(inputDoc).blockingGet();
        assertThat(newDoc.getString("c")).isEqualTo("m");
        assertThat(newDoc.getString("ca")).isEqualTo("me");
        assertThat(newDoc.getString("cat")).isEqualTo("meo");
        assertThat(newDoc.getString("cats")).isEqualTo("meow");
        testContext.completeNow();
    }
}

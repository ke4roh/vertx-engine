package com.redhat.vertx.pipeline.step;

import java.util.concurrent.TimeUnit;

import com.redhat.ResourceUtils;
import com.redhat.vertx.Engine;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.reactivex.core.Vertx;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(VertxExtension.class)
public class VariableSubstitutionIntegrationTest {
    @Test
    public void basicVariableSubstitution(Vertx vertx, VertxTestContext testContext) {
        JsonObject doc = new JsonObject(
                ResourceUtils.fileContentsFromResource(
                        "com/redhat/vertx/pipeline/step/varSubstitutionTestDoc.json"
                ));
        Engine engine = new Engine(
                ResourceUtils.fileContentsFromResource(
                        "com/redhat/vertx/pipeline/step/varSubstitutionTestPipeline.json"
                ));
        vertx.rxDeployVerticle(engine).timeout(500, TimeUnit.MILLISECONDS).blockingGet();
        JsonObject d2 = engine.execute(doc).timeout(1000, TimeUnit.MILLISECONDS).blockingGet();  // TODO faster
        assertEquals("This",d2.getString("first_word"));
        assertEquals("{{var}}", d2.getString("fourth_word"));
        testContext.completeNow();
    }
}

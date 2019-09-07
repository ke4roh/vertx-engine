package com.redhat.vertx.pipeline;

import com.redhat.ResourceUtils;
import com.redhat.vertx.Engine;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.reactivex.core.Vertx;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(VertxExtension.class)
public class IncompletePipelineTest {
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
}

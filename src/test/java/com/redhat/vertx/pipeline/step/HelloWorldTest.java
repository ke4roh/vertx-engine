package com.redhat.vertx.pipeline.step;

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
public class HelloWorldTest {

    @Test
    public void checkHelloWorld(Vertx vertx, VertxTestContext testContext) throws Exception {
        Engine e = new Engine(ResourceUtils.fileContentsFromResource("hello-world-pipeline.json"));
        vertx.rxDeployVerticle(e).timeout(1,TimeUnit.SECONDS).blockingGet();
        JsonObject newDoc = e.execute(new JsonObject()).timeout(1,TimeUnit.SECONDS).blockingGet();
        assertThat(newDoc.getString(Engine.DOC_UUID)).isNotBlank();
        assertThat(newDoc.getString("greetings")).isEqualTo("hello, Jason");
        testContext.completeNow();
    }

    @Test
    public void checkHelloWorldMultiSteps(Vertx vertx, VertxTestContext testContext) throws Exception {
        Engine e = new Engine(ResourceUtils.fileContentsFromResource("hello-world-many-steps-pipeline.json"));
        vertx.rxDeployVerticle(e).timeout(1,TimeUnit.SECONDS).blockingGet();
        JsonObject newDoc = e.execute(new JsonObject()).timeout(1,TimeUnit.SECONDS).blockingGet();
        assertThat(newDoc.getString(Engine.DOC_UUID)).isNotBlank();
        assertThat(newDoc.getString("greetings")).isEqualTo("hello, Jason");
        assertThat(newDoc.getString("nuisance")).isEqualTo("hello, Banana");
        assertThat(newDoc.getString("rainbow")).isEqualTo("hello, Blue");
        testContext.completeNow();
    }

    @Test
    public void checkHelloWorldNestedSections(Vertx vertx, VertxTestContext testContext) throws Exception {
        Engine e = new Engine(ResourceUtils.fileContentsFromResource("hello-world-nested-sections-pipeline.json"));
        vertx.rxDeployVerticle(e).blockingGet();
        JsonObject newDoc = e.execute(new JsonObject()).timeout(1, TimeUnit.SECONDS).blockingGet();
        assertThat(newDoc.getString(Engine.DOC_UUID)).isNotBlank();
        assertThat(newDoc.getString("greetings")).isEqualTo("hello, Jason");
        assertThat(newDoc.getString("nuisance")).isEqualTo("hello, Banana");
        assertThat(newDoc.getString("rainbow")).isEqualTo("hello, Blue");
        testContext.completeNow();
    }

}

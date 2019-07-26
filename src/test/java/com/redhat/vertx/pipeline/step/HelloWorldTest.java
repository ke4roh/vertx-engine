package com.redhat.vertx.pipeline.step;

import java.util.List;

import com.redhat.ResourceUtils;
import com.redhat.vertx.Engine;
import io.reactivex.observers.TestObserver;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.reactivex.core.Vertx;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(VertxExtension.class)
public class HelloWorldTest {

    @Test
    public void checkHelloWorld(Vertx vertx, VertxTestContext testContext) throws Exception {
        Engine e = new Engine(ResourceUtils.fileContentsFromResource("hello-world-pipeline.json"));
        vertx.rxDeployVerticle(e).blockingGet();
        JsonObject newDoc = e.execute(new JsonObject()).blockingGet();
        assertThat(newDoc.getString(Engine.DOC_UUID)).isNotBlank();
        assertThat(newDoc.getString("greetings")).isEqualTo("hello, Jason");
        testContext.completeNow();
    }
}

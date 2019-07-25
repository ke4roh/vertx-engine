package com.redhat.vertx.pipeline.step;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import com.redhat.ResourceUtils;
import com.redhat.vertx.Engine;
import io.vertx.core.json.JsonObject;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

public class HelloWorldTest {
    @Test
    public void checkHelloWorld() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        Engine e = new Engine(ResourceUtils.fileContentsFromResource("hello-world-pipeline.json"));

        e.execute(new JsonObject()).subscribe((entries, throwable) -> {
            if (throwable != null)
                fail("Failed", throwable);

            assertThat(entries).isNotNull();
            assertThat(entries.getString("hello")).isEqualTo("hello, Jason");
            latch.countDown();
        });

        latch.await(1, TimeUnit.SECONDS);
    }
}

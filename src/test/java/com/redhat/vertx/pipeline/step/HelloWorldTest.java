package com.redhat.vertx.pipeline.step;

import java.util.List;
import java.util.concurrent.TimeUnit;

import com.redhat.ResourceUtils;
import com.redhat.vertx.Engine;
import io.reactivex.observers.TestObserver;
import io.vertx.core.json.JsonObject;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class HelloWorldTest {
    @Test
    public void checkHelloWorld() throws Exception {
        Engine e = new Engine(ResourceUtils.fileContentsFromResource("hello-world-pipeline.json"));

        TestObserver<Object> testObserver = new TestObserver<>();

        e.execute(new JsonObject())
                .subscribeWith(testObserver)
                .assertSubscribed()
                .assertValueCount(1)
                .awaitDone(1, TimeUnit.SECONDS);

        List<Object> values = testObserver.values();

        assertThat(values).isNotNull();
        assertThat(((JsonObject) values.get(0)).getString("default")).isEqualTo("hello, Jason");
    }
}

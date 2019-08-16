package com.redhat.vertx.pool;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import com.redhat.vertx.Engine;
import io.reactivex.Single;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

public class EnginePoolTest {

    @Test
    public void testWithFailingResolver() throws Exception {
        EnginePool pool = new EnginePool(new FailingPipelineResolver());
        Single<Engine> future = pool.getEngineByPipelineName("foo");
        CountDownLatch latch = new CountDownLatch(1);
        future.subscribe(
                engine -> fail("Exception must be thrown."),
                err -> {
                    assertEquals(RuntimeException.class, err.getCause().getClass());
                    assertEquals("You shall not pass.", err.getCause().getMessage());
                    latch.countDown();
                });

        latch.await(500, TimeUnit.MILLISECONDS);
    }

    public static class FailingPipelineResolver implements PipelineResolver {
        @Override
        public Single getExecutablePipelineByName(String pipelineName) {
            return Single.error(new RuntimeException("You shall not pass."));
        }
    }
}

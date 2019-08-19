package com.redhat.vertx.pool;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import com.redhat.vertx.Engine;
import io.reactivex.Single;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.reactivex.core.Vertx;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

@ExtendWith(VertxExtension.class)
public class EnginePoolTest {

    @Test
    public void testWithFailingResolver(Vertx vertx, VertxTestContext testContext) throws Exception {
        EnginePool pool = new EnginePool(new FailingPipelineResolver(),vertx);
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
        testContext.completeNow();
    }

    public static class FailingPipelineResolver implements PipelineResolver {
        @Override
        public Single getExecutablePipelineByName(String pipelineName) {
            return Single.error(new RuntimeException("You shall not pass."));
        }
    }
}

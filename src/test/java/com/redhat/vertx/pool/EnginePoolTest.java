package com.redhat.vertx.pool;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import com.redhat.ResourceUtils;
import com.redhat.vertx.Engine;
import io.reactivex.Single;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.reactivex.core.Vertx;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

@ExtendWith(VertxExtension.class)
public class EnginePoolTest {

    @Test
    public void testWithFailingResolver(Vertx vertx, VertxTestContext testContext) throws Exception {
        EnginePool pool = new EnginePool(new FailingPipelineResolver(), vertx);
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

    @Test
    public void testUnregister(Vertx vertx, VertxTestContext testContext) throws Exception {
        var checkpoint = testContext.checkpoint(2);

        var expectedDeployments = 2;
        var pool = new EnginePool(
                pipelineName -> Single.just(ResourceUtils.fileContentsFromResource("hello-world-pipeline.json")),
                vertx
        );

        pool.getEngineByPipelineName("hello-world-pipeline").blockingGet();

        assertThat(vertx.deploymentIDs().size()).isEqualTo(expectedDeployments); // DocumentLogger is always being deployed
        checkpoint.flag();

        pool.cleanupAll().blockingGet();

        assertThat(vertx.deploymentIDs().size()).isLessThan(expectedDeployments); // Should ideally be 1 less than expectedDeployments
        checkpoint.flag();

        testContext.awaitCompletion(500, TimeUnit.MILLISECONDS);
    }

    public static class FailingPipelineResolver implements PipelineResolver {
        @Override
        public Single getExecutablePipelineByName(String pipelineName) {
            return Single.error(new RuntimeException("You shall not pass."));
        }
    }
}

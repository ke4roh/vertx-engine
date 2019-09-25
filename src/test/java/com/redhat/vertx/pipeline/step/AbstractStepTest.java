package com.redhat.vertx.pipeline.step;

import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Logger;

import com.redhat.ResourceUtils;
import com.redhat.vertx.Engine;
import com.redhat.vertx.pipeline.AbstractStep;
import com.redhat.vertx.pipeline.Step;
import com.redhat.vertx.pipeline.templates.MissingParameterException;
import io.reactivex.Maybe;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.reactivex.MaybeHelper;
import io.vertx.reactivex.core.Vertx;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.kohsuke.MetaInfServices;
import org.opentest4j.AssertionFailedError;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(VertxExtension.class)
public class AbstractStepTest {

    @MetaInfServices(Step.class)
    public static class Concat extends AbstractStep {
        Logger logger = Logger.getLogger(this.getClass().getName());

        @Override
        public Object execute(JsonObject doc) {
            String base = doc.getString("from");
            if (base==null) {
                throw new MissingParameterException();
            }
            logger.finest(() -> "Step " + name + " executing.");
            return base + doc.getString("append");
        }
    }

    @Test
    public void testSequencingStepsWithIncompleteEnvironments(Vertx vertx, VertxTestContext testContext) throws Exception {
        Engine e = new Engine(ResourceUtils.fileContentsFromResource("abstract-step-test-pipeline.json"));
        vertx.rxDeployVerticle(e).blockingGet();
        JsonObject inputDoc = new JsonObject().put("x","");
        JsonObject newDoc = e.execute(inputDoc).timeout(1, TimeUnit.SECONDS).blockingGet();
        assertThat(newDoc.getString("c")).isEqualTo("m");
        assertThat(newDoc.getString("ca")).isEqualTo("me");
        assertThat(newDoc.getString("cat")).isEqualTo("meo");
        assertThat(newDoc.getString("cats")).isEqualTo("meow");
        testContext.completeNow();
    }

    @Test
    public void testExceptionInExecuteSlow(Vertx vertx, VertxTestContext testContext) throws InterruptedException {
        /* Supposing a RuntimeException occurs inside executeSlow before it constructs its Maybe,
         * does it get handled properly and put onto the Maybe from the step?
         */
        String message = "Not gonna happen";
        AbstractStep step = new AbstractStep() {
            @Override
            protected Maybe<Object> executeSlow(JsonObject env) {
                throw new MissingParameterException(message);
            }
        };
        String doc_id = UUID.randomUUID().toString();
        JsonObject doc = new JsonObject();
        Engine engineMock = mock(Engine.class);
        when(engineMock.getDocument(doc_id)).thenReturn(doc);
        when(engineMock.getRxVertx()).thenReturn(vertx);

        step.init(engineMock,new JsonObject());

        step.execute(doc_id).subscribe(
                s -> needExceptionFinishTest(s, message, testContext, MissingParameterException.class),
                e -> needExceptionFinishTest(e, message, testContext, MissingParameterException.class),
                () -> needExceptionFinishTest(null,message,testContext, MissingParameterException.class) );
    }

    private void needExceptionFinishTest(Object o, String message, VertxTestContext testContext, Class<? extends Throwable> expectedException) {
        assertThat(o).isInstanceOf(expectedException);
        if (message != null) {
            assertThat(((Exception) o).getMessage()).isEqualTo(message);
        }
        testContext.completeNow();
    }

    @Test
    public void testTimeout(Vertx vertx, VertxTestContext testContext) {
        AbstractStep step = new AbstractStep() {
            @Override
            protected Maybe<Object> executeSlow(JsonObject env) {
                return MaybeHelper.toMaybe(handler -> {
                    vertx.executeBlocking(fut -> fut.complete(sleepALongTime()), handler);
                });
            }

            private Object sleepALongTime() {
                try {
                    Thread.sleep(525600l * 20 * 60 * 1000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                return null;
            }
        };

        String doc_id = UUID.randomUUID().toString();
        JsonObject doc = new JsonObject();
        Engine engineMock = mock(Engine.class);
        when(engineMock.getDocument(doc_id)).thenReturn(doc);
        when(engineMock.getRxVertx()).thenReturn(vertx);

        step.init(engineMock,new JsonObject().put("timeout","PT0.050S"));

        step.execute(doc_id)
                .timeout(3,TimeUnit.SECONDS, source -> source.onError(new AssertionFailedError("Too slow.")))
                .subscribe(
                s -> needExceptionFinishTest(s, null, testContext, TimeoutException.class),
                e -> needExceptionFinishTest(e, null, testContext, TimeoutException.class),
                () -> needExceptionFinishTest(null,null,testContext, TimeoutException.class) );

    }

    @Test
    public void testStepAsPartOfEnvironment(Vertx vertx, VertxTestContext testContext) {
        Engine e = new Engine(ResourceUtils.fileContentsFromResource("com/redhat/vertx/pipeline/step/step-part-of-env.yml"));
        vertx.rxDeployVerticle(e).blockingGet();
        JsonObject inputDoc = new JsonObject().put("x","");
        JsonObject newDoc = e.execute(inputDoc).timeout(1, TimeUnit.SECONDS).blockingGet();
        assertThat(newDoc.getString("hello")).isEqualTo("Hello from the \"step name check\" step");
        testContext.completeNow();
    }
}

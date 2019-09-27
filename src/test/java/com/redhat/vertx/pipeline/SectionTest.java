package com.redhat.vertx.pipeline;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.redhat.ResourceUtils;
import com.redhat.vertx.Engine;
import com.redhat.vertx.pipeline.json.YamlParser;
import com.redhat.vertx.pipeline.steps.Copy;
import com.redhat.vertx.pipeline.templates.MissingParameterException;
import io.reactivex.Completable;
import io.reactivex.Maybe;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.Timeout;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.reactivex.core.Vertx;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.kohsuke.MetaInfServices;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.mock;

@ExtendWith(VertxExtension.class)
class SectionTest {

    @Test
    void testUnknownKeyException() {
        var section = new Section();
        final var pipelineDef = ResourceUtils.fileContentsFromResource("com/redhat/vertx/pipeline/short-name-pipeline-unknown-key.yml");
        assertThatExceptionOfType(RuntimeException.class).
                isThrownBy(() -> section.init(null, new JsonObject(YamlParser.parse(pipelineDef))).blockingGet())
                .withMessage("Unknown keys in configuration: [sleep, foo]");
    }

    @Test
    void testNoStepImpl() {
        var section = new Section();
        final var pipelineDef = ResourceUtils.fileContentsFromResource("com/redhat/vertx/pipeline/unknown-step.yml");
        assertThatExceptionOfType(RuntimeException.class).
                isThrownBy(() -> section.init(null, new JsonObject(YamlParser.parse(pipelineDef))).blockingGet())
                .withMessage("Error locating step implementation");
    }

    @Test
    void testShortNameStepInit() throws NoSuchFieldException, IllegalAccessException {
        var section = new Section();
        final var pipelineDef = ResourceUtils.fileContentsFromResource("com/redhat/vertx/pipeline/short-name-pipeline.yml");
        section.init(null, new JsonObject(YamlParser.parse(pipelineDef))).blockingGet();

        // Keeping the field private, but I need to do asserts against it
        var stepsField = Section.class.getDeclaredField("steps");
        assertThat(stepsField.trySetAccessible()).isTrue();



        @SuppressWarnings("unchecked")
        List<StepExecutor> steps = (List<StepExecutor>) stepsField.get(section);

        assertThat(steps).hasSize(4);
        assertThat(steps.get(0).step).isInstanceOf(PipelineIntegrationTest.Sleep.class);
        assertThat(steps.get(1).step).isInstanceOf(Copy.class);
        assertThat(steps.get(2).step).isInstanceOf(Copy.class);
        assertThat(steps.get(3).step).isInstanceOf(Copy.class);
    }

    @MetaInfServices(Step.class)
    public static class LongSleep extends AbstractStep {
        @Override
        public Maybe<Object> execute(JsonObject env) {
            return Completable.timer(20, TimeUnit.DAYS).toMaybe();
        }
    }

    @Test
    @Timeout(value = 2, timeUnit = TimeUnit.SECONDS)
    public void testTimeout(Vertx vertx, VertxTestContext testContext) {
        String pipelineDef = "[ { long_sleep:{ }, timeout: \"0.250S\" } ]";
        Engine e = new Engine(pipelineDef,new JsonObject());
        vertx.rxDeployVerticle(e).blockingGet();
        long startTime = System.currentTimeMillis();
        // 4x so that execution time >> parse time
        for (int i=0;i<4;i++) {
            assertThatExceptionOfType(RuntimeException.class)
                    .isThrownBy(() -> e.execute(new JsonObject()).blockingGet())
                    .withCauseExactlyInstanceOf(TimeoutException.class);
        }
        assertThat(System.currentTimeMillis() - startTime).isLessThan(2000);
        testContext.completeNow();
    }

    @MetaInfServices(Step.class)
    public static class MissingParameterStep extends AbstractStep {
        @Override
        public Maybe<Object> execute(JsonObject env) {
            throw new MissingParameterException();
        }
    }


    @Test
    public void testExceptionInExecuteSlow(Vertx vertx, VertxTestContext testContext) throws InterruptedException {
        /* Supposing a RuntimeException occurs inside execute before it constructs its Maybe,
         * does it get handled properly and put onto the Maybe from the step?
         */
        String pipelineDef = "[ { missing_parameter_step:{ } } ]";
        Engine e = new Engine(pipelineDef,new JsonObject());
        vertx.rxDeployVerticle(e).blockingGet();
        long startTime = System.currentTimeMillis();
        assertThatExceptionOfType(MissingParameterException.class)
                .isThrownBy(() -> e.execute(new JsonObject()).blockingGet());
        assertThat(System.currentTimeMillis() - startTime).isLessThan(1000);
        testContext.completeNow();
    }
}

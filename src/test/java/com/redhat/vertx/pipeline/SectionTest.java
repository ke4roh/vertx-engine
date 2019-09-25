package com.redhat.vertx.pipeline;

import java.util.List;

import com.redhat.ResourceUtils;
import com.redhat.vertx.pipeline.json.YamlParser;
import com.redhat.vertx.pipeline.steps.Copy;
import io.vertx.core.json.JsonObject;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

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
        List<Step> steps = (List<Step>) stepsField.get(section);

        assertThat(steps).hasSize(4);
        assertThat(steps.get(0)).isInstanceOf(PipelineIntegrationTest.Sleep.class);
        assertThat(steps.get(1)).isInstanceOf(Copy.class);
        assertThat(steps.get(2)).isInstanceOf(Copy.class);
        assertThat(steps.get(3)).isInstanceOf(Copy.class);
    }
}

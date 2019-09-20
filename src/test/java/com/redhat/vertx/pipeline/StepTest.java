package com.redhat.vertx.pipeline;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class StepTest {

    class LongNameClassNameTester extends AbstractStep {
    }

    @Test
    void getShortName() {
        final Step testClass = new LongNameClassNameTester();

        assertThat(testClass.getShortName()).isEqualTo("long_name_class_name_tester");
    }
}

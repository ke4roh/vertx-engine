package com.redhat.vertx.pipeline.templates;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class TestJinjaRe {
    @Test
    public void testMatch() {
        String regex =  "^.{0,1000}(?:\\b|$)";
        String body = "Why are custom channels not syncing properly from my ISS master?";

        String result = JinjaRe.match(body, regex);
        assertThat(result).isEqualTo(body);
    }
}

package com.redhat.vertx.pipeline.templates;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.StreamHandler;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class TestJinjaTemplateProcessor {
    private static Logger log = Logger.getLogger(JinjaTemplateProcessor.class.getName());
    private static OutputStream logCapturingStream;
    private static StreamHandler customLogHandler;

    @BeforeEach
    public void attachLogCapturer() {
        logCapturingStream = new ByteArrayOutputStream();
        Handler[] handlers = log.getParent().getHandlers();
        customLogHandler = new StreamHandler(logCapturingStream, handlers[0].getFormatter());
        log.addHandler(customLogHandler);
    }

    @Test
    public void testLogFatal() throws Exception {
        var processor = new JinjaTemplateProcessor();
        processor.applyTemplate(Collections.emptyMap(), "{{ greeting }} User!");

        var capturedLog = getTestCapturedLog();
        assertThat(capturedLog).isNotEmpty();
        assertThat(capturedLog).contains("FATAL");
        assertThat(capturedLog).contains("message='UnknownTokenException: Unknown token found: greeting'");
    }

    @Test
    public void testLogWarning() throws Exception {
        var processor = new JinjaTemplateProcessor();
        var context = new HashMap<String, Object>();
        context.put("greeting", new Greeter("Jim"));
        log.setLevel(Level.ALL);

        processor.applyTemplate(context, "Hello {{ greeting.lastName }}!");

        var capturedLog = getTestCapturedLog();
        assertThat(capturedLog).isNotEmpty();
        assertThat(capturedLog).contains("WARNING");
        assertThat(capturedLog).contains("message='Cannot resolve property 'lastName' in 'Greeter''");
    }

    @Test
    public void testRegexMatch() throws Exception {
        var processor = new JinjaTemplateProcessor();
        var context = new HashMap<String, Object>();
        context.put("month","November");

        assertThat(processor.applyTemplate(context, "{{ re:m(month,\"Nov\") }}")).isEqualTo("Nov");
        assertThat(processor.applyTemplate(context, "{{ re:m(month,\"Jan\") }}")).isEmpty();
    }

    @Test
    public void testRegExFilter() throws Exception {
        var processor = new JinjaTemplateProcessor();
        var context = new HashMap<String, Object>();
        context.put("month","November");

        assertThat(processor.applyTemplate(context, "{{ month | reMatch(\"Nov\") }}")).isEqualTo("Nov");
        assertThat(processor.applyTemplate(context, "{{ month | reMatch(\"Jan\") }}")).isEmpty();
    }

    @Test
    public void testRegSubFilter() throws Exception {
        var processor = new JinjaTemplateProcessor();
        var context = new HashMap<String, Object>();
        context.put("fruit","Banana");
        context.put("nut","Cashew");

        assertThat(processor.applyTemplate(context, "{{ fruit | reSub(\"na\",\"go\") }}")).isEqualTo("Bagogo");
        assertThat(processor.applyTemplate(context, "{{ nut | reSub(\"na\",\"go\") }}")).isEqualTo(context.get("nut"));
    }

    public String getTestCapturedLog() throws IOException {
        customLogHandler.flush();
        return logCapturingStream.toString();
    }

    public static class Greeter {
        String name;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Greeter(String name) {
            this.name = name;
        }
    }

}

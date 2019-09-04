package com.redhat.vertx.pipeline.templates;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.StreamHandler;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

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
    public void throwExceptionForMissingVariable() throws IOException {
        JinjaTemplateProcessor p = new JinjaTemplateProcessor();
        try {
            p.applyTemplate(Collections.emptyMap(), "{{not_there}}");
            fail("Expected exception");
        } catch (MissingParameterException e) {
            assertThat(e.getMessage()).isEqualTo("not_there");
        }
        assertThat(getTestCapturedLog()).isEmpty();
    }

    @Test
    public void throwExceptionForMissingVariableDeeper() throws IOException {
        JinjaTemplateProcessor p = new JinjaTemplateProcessor();
        Map<String, Object> m = new HashMap<>();
        m.put("really", Collections.emptyMap());
        try {
            p.applyTemplate(m, "{{really.not_there}}");
            fail("Expected exception");
        } catch (MissingParameterException e) {
            assertThat(e.getMessage()).isEqualTo("really.not_there");
        }
        assertThat(getTestCapturedLog()).isEmpty();
    }

    @Test
    public void testLogFatal() throws Exception {
        var processor = new JinjaTemplateProcessor();
        processor.applyTemplate(Collections.emptyMap(), "{{ gr@$%^#%^eeting }} User!");

        var capturedLog = getTestCapturedLog();
        assertThat(capturedLog).isNotEmpty();
        assertThat(capturedLog).contains("FATAL");
        assertThat(capturedLog).contains("message='Error parsing 'gr@$%^#%^eeting': lexical error at position 5");
    }

    @Test
    public void testLogWarning() throws Exception {
        var processor = new JinjaTemplateProcessor();
        var context = new HashMap<String, Object>();
        context.put("greeting","{{salutations}}");
        context.put("salutations","{{greeting}}");
        log.setLevel(Level.ALL);

        processor.applyTemplate(context, "{{greeting}}");

        var capturedLog = getTestCapturedLog();
        assertThat(capturedLog).isNotEmpty();
        assertThat(capturedLog).contains("WARNING");
        assertThat(capturedLog).contains("message='Rendering cycle detected: '{{greeting}}''");
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
    public void testRegexSearchFilter() {
        var processor = new JinjaTemplateProcessor();
        var context = new HashMap<String, Object>();
        context.put("trim", "^.{0,1000}(?:\\b|$)");
        context.put("summary", "Why are custom channels not syncing properly from my ISS master?");

        var expected = "Why are custom channels not syncing properly from my ISS master?";

        assertThat(processor.applyTemplate(context, "{{ summary | regex_search(trim) }}")).isEqualTo(expected);
    }

    @Test
    public void testRegExFilter() throws Exception {
        var processor = new JinjaTemplateProcessor();
        var context = new HashMap<String, Object>();
        context.put("month","November");

        assertThat(processor.applyTemplate(context, "{{ month | regex_search(\"Nov\") }}")).isEqualTo("Nov");
        assertThat(processor.applyTemplate(context, "{{ month | regex_search(\"Jan\") }}")).isEmpty();
    }

    @Test
    public void testRegSubFilter() throws Exception {
        var processor = new JinjaTemplateProcessor();
        var context = new HashMap<String, Object>();
        context.put("fruit","Banana");
        context.put("nut","Cashew");

        assertThat(processor.applyTemplate(context, "{{ fruit | regex_replace(\"na\",\"go\") }}")).isEqualTo("Bagogo");
        assertThat(processor.applyTemplate(context, "{{ nut | regex_replace(\"na\",\"go\") }}")).isEqualTo(context.get("nut"));
    }

    @Test
    public void testRegSubFilterWithNamedArray() throws Exception {
        var processor = new JinjaTemplateProcessor();
        var context = new HashMap<String, Object>();
        context.put("fruit","Banana");
        context.put("nut","Cashew");
        context.put("na_go", Arrays.asList("na","go"));

        assertThat(processor.applyTemplate(context, "{{ fruit | regex_replace(na_go) }}")).isEqualTo("Bagogo");
        assertThat(processor.applyTemplate(context, "{{ nut | regex_replace(na_go) }}")).isEqualTo(context.get("nut"));
    }

    public String getTestCapturedLog() throws IOException {
        customLogHandler.flush();
        return logCapturingStream.toString();
    }

}

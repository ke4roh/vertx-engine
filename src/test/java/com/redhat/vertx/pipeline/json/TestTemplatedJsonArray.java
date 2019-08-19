package com.redhat.vertx.pipeline.json;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

import com.redhat.vertx.pipeline.templates.JinjaTemplateProcessor;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

public class TestTemplatedJsonArray {
    @Test
    public void testImmutable() {
        var arr1 = new TemplatedJsonArray(new JsonArray(), new JinjaTemplateProcessor(), new JsonObject());

        assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> arr1.add(TimeUnit.SECONDS));
        assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> arr1.add(1L));
        assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> arr1.add(1f));
        assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> arr1.add(new byte[0]));
        assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> arr1.add(1.2));
        assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> arr1.add(""));
        assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> arr1.add(false));
        assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> arr1.add(1));
        assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> arr1.add(Instant.now()));
        assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> arr1.addAll(new JsonArray()));
        assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> arr1.add(new JsonArray()));
        assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> arr1.add(new JsonObject()));
        assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> arr1.add(new StringBuffer()));
        assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> arr1.add(new Object()));
        assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> arr1.remove(new Object()));
        assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> arr1.remove(0));
        assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> arr1.readFromBuffer(0, Buffer.buffer()));
        assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(arr1::clear);
        assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(arr1::addNull);
    }

    @Test
    public void testGet() throws NoSuchAlgorithmException {
        byte[] bytes = new byte[20];
        SecureRandom.getInstanceStrong().nextBytes(bytes);

        var backingArr = new JsonArray()
                .add(TimeUnit.SECONDS)                          // 0
                .add(1L)                                        // 1
                .add(1f)                                        // 2
                .add(1)                                         // 3
                .add(1.2)                                       // 4
                .add(bytes)                                     // 5
                .add("")                                        // 6
                .add(true)                                      // 7
                .add(Instant.parse("2019-08-19T14:15:00.000Z")) // 8
                .add(new JsonArray().add("hi"))                 // 9
                .add(new JsonObject().put("text", "hi"))        // 10
                .add(new StringBuffer("testing"))               // 11
                .addNull();                                     // 12
        var arr = new TemplatedJsonArray(backingArr, new JinjaTemplateProcessor(), new JsonObject());

        assertThat(arr.getValue(0)).isEqualTo(TimeUnit.SECONDS.toString());
        assertThat(arr.getLong(1)).isEqualTo(1L);
        assertThat(arr.getFloat(2)).isEqualTo(1f);
        assertThat(arr.getInteger(3)).isEqualTo(1);
        assertThat(arr.getDouble(4)).isEqualTo(1.2);
        assertThat(arr.getBinary(5)).isEqualTo(bytes);
        assertThat(arr.getString(6)).isEqualTo("");
        assertThat(arr.getBoolean(7)).isEqualTo(true);
        assertThat(arr.getInstant(8)).isEqualTo(Instant.parse("2019-08-19T14:15:00.000Z"));
        assertThat(arr.getJsonArray(9).getList()).isEqualTo(new JsonArray().add("hi").getList());
        assertThat(arr.getJsonObject(10).getMap()).isEqualTo(new JsonObject().put("text", "hi").getMap());
        assertThat(arr.getValue(11)).isNotNull();
        assertThat(arr.hasNull(12)).isTrue();
    }
}

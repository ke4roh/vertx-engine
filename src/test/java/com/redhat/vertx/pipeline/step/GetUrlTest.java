package com.redhat.vertx.pipeline.step;

import com.redhat.vertx.Engine;
import com.redhat.vertx.pipeline.json.TemplatedJsonObject;
import com.redhat.vertx.pipeline.steps.BaseHttpClient;
import com.redhat.vertx.pipeline.steps.GetUrl;
import com.redhat.vertx.pipeline.templates.JinjaTemplateProcessor;
import com.redhat.vertx.pipeline.templates.MissingParameterException;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.core.buffer.Buffer;
import io.vertx.reactivex.ext.web.client.HttpResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.opentest4j.AssertionFailedError;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(VertxExtension.class)
public class GetUrlTest {
    @Test
    public void testGetUrl(Vertx vertx, VertxTestContext testContext) throws Exception {
        JsonObject env = new JsonObject();
        String url = "http://www.example.com/";
        env.put("url",url);
        BaseHttpClient client = new GetUrl();
        assertThat(client.getUrl(env)).isEqualTo(url);

        env = new TemplatedJsonObject(new JsonObject(), new JinjaTemplateProcessor());
        try {
            client.getUrl(env).length();
            assertThat(false).isTrue();
        } catch (MissingParameterException e) {
            // expected
        }
        testContext.completeNow();
    }

    @Test
    public void testGetRequest(Vertx vertx, VertxTestContext testContext) throws Exception {
        JsonObject env = new JsonObject();
        String url = "http://www.example.com/";
        env.put("url",url);
        BaseHttpClient client = new GetUrl();

        Engine engine = mock(Engine.class);
        when(engine.getRxVertx()).thenReturn(vertx);
        client.init(engine,new JsonObject());

        assertThat(client.request(env)).isNotNull();

        env = new TemplatedJsonObject(new JsonObject(), new JinjaTemplateProcessor());
        try {
            client.request(env);
            fail();
        } catch (MissingParameterException e) {
            // expected
        }
        testContext.completeNow();
    }

    @Test
    public void testProcessResponse(Vertx vertx, VertxTestContext testContext) throws Exception {
        JsonObject env = new JsonObject();
        String url = "http://www.example.com/";
        env.put("url",url);
        BaseHttpClient client = new GetUrl();

        Engine engine = mock(Engine.class);
        when(engine.getRxVertx()).thenReturn(vertx);
        client.init(engine,new JsonObject());


        HttpResponse<Buffer> response = mock(HttpResponse.class);
        JsonObject expectedBody = new JsonObject().put("foo","bar");
        when(response.body()).thenReturn(Buffer.newInstance(expectedBody.toBuffer()));
        when(response.getHeader("Content-type")).thenReturn("application/json");
        when(response.bodyAsJsonObject()).thenReturn(expectedBody);
        when(response.statusCode()).thenReturn(200);
        Object body = client.processResponse(response);
        assertThat(body).isEqualTo(expectedBody);

        response = mock(HttpResponse.class);
        when(response.body()).thenReturn(null);
        when(response.statusCode()).thenReturn(204);
        body = client.processResponse(response);
        assertThat(body).isNull();

        response = mock(HttpResponse.class);
        when(response.body()).thenThrow(AssertionFailedError.class);
        when(response.statusCode()).thenReturn(503);
        try {
            body = client.processResponse(response);
            fail();
        } catch (BaseHttpClient.HttpResponseStatusException e) {
            // expected
        }

        testContext.completeNow();
    }


}

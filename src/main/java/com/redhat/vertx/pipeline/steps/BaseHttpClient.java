package com.redhat.vertx.pipeline.steps;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.function.Function;
import java.util.logging.Logger;

import com.redhat.vertx.Engine;
import com.redhat.vertx.pipeline.AbstractStep;
import com.redhat.vertx.pipeline.templates.MissingParameterException;
import io.reactivex.Completable;
import io.reactivex.Maybe;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.reactivex.core.buffer.Buffer;
import io.vertx.reactivex.ext.web.client.HttpRequest;
import io.vertx.reactivex.ext.web.client.HttpResponse;
import io.vertx.reactivex.ext.web.client.WebClient;

public abstract class BaseHttpClient extends AbstractStep {
    private static Logger logger = Logger.getLogger(BaseHttpClient.class.getName());
    private WebClient http;

    public String getUrl(JsonObject env) {
        String url = env.getString("url");
        if ( url == null ) {
            throw new MissingParameterException("url");
        }
        return url;
    }

    @Override
    public Completable init(Engine engine, JsonObject config) {
        config.put("timeout",config.getString("timeout", "PT30.000S"));
        return super.init(engine, config);
    }

    public abstract HttpRequest<Buffer> request(JsonObject env);

    public Object processResponse(HttpResponse<Buffer> response) throws HttpResponseStatusException {
        switch (response.statusCode()) {
            case 200: // OK
                return decodeResponse(response);
            case 204: // no content
                return null;
            default:
                throw new HttpResponseStatusException(response);
        }
    }

    public static final Map<String, Function<HttpResponse<Buffer>,Object>> decodings;
    static {
        Map<String, Function<HttpResponse<Buffer>,Object>> d = Map.of(
        "text/plain",HttpResponse::bodyAsString,
        "text/html",HttpResponse::bodyAsString,
        "application/json", HttpResponse::bodyAsJsonObject, // TODO manage JsonArray and String as appropriate
        "application/xml", HttpResponse::bodyAsString); // TODO parse and shoehorn into JSON
        decodings = Collections.unmodifiableMap(d);
    }

    public Object decodeResponse(HttpResponse<Buffer> response) {
        if (response.body().length() == 0) {
            return null;
        }
        String contentType = response.getHeader("Content-type");

        return decodings.getOrDefault(contentType,HttpResponse::bodyAsString).apply(response);
    }

    public Maybe<Object> rxProcessResponse(HttpResponse<Buffer> response) {
        try {
            Object body = processResponse(response);
            return (body == null)?Maybe.empty():Maybe.just(body);
        } catch (Exception e) {
            return Maybe.error(e);
        }
    }

    @Override
    public Maybe<Object> execute(JsonObject env) {
        HttpRequest<Buffer> request;
        try {
            request = request(env);
        } catch (Exception e) {
            return Maybe.error(e);
        }

        return request.rxSend().flatMapMaybe(this::rxProcessResponse);
    }

    protected WebClient webClient() {
        if (http == null) {
            WebClientOptions options = new WebClientOptions()
                    .setUserAgent("vertx-engine")
                    .setKeepAlive(true)
                    .setConnectTimeout(30)
                    .setKeepAliveTimeout(300)
                    .setIdleTimeout(300);
            http = WebClient.create(getVertx(),options);
        }

        return http;
    }

    public static class HttpResponseStatusException extends IOException {
        public final HttpResponse<?> response;

        HttpResponseStatusException(HttpResponse<?> response) {
            super(response.statusCode() + " " + response.statusMessage());
            this.response=response;
        }
    }
}

package com.redhat.vertx.pipeline.steps;

import java.net.URI;
import java.util.logging.Logger;

import com.redhat.vertx.pipeline.Step;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.core.buffer.Buffer;
import io.vertx.reactivex.ext.web.client.HttpRequest;
import org.kohsuke.MetaInfServices;

@MetaInfServices(Step.class)
public class GetUrl extends BaseHttpClient {
    private static Logger logger = Logger.getLogger(GetUrl.class.getName());

    @Override
    public HttpRequest<Buffer> request(JsonObject env) {
            String url = getUrl(env);
            logger.fine(() -> "requesting " + url);
            URI uri = URI.create(url);
            String pqf = uri.getPath();

            if (uri.getQuery() != null) {
                pqf += "?" + uri.getQuery();
            }
            if (uri.getFragment() != null) {
                pqf += "#" + uri.getFragment();
            }
            return webClient()
                    .request(HttpMethod.GET, uri.getPort(), uri.getHost(), pqf)
                    .putHeader("Accept","application/json");
    }
}

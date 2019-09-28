package com.redhat.vertx.pipeline.step;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Handler;
import java.util.logging.Logger;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.redhat.ResourceUtils;
import com.redhat.vertx.Engine;
import com.redhat.vertx.pipeline.steps.HttpClient;
import io.reactivex.Maybe;
import io.reactivex.disposables.Disposable;
import io.reactivex.internal.disposables.DisposableHelper;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.ext.unit.TestContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.opentest4j.AssertionFailedError;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(VertxExtension.class)
public class HttpClientIntegrationTest {
    private WireMockServer wireMockServer;

    @BeforeEach
    public void setup() {
        wireMockServer = new WireMockRule(wireMockConfig().dynamicPort().dynamicHttpsPort());
        wireMockServer.start();
        WireMock.configureFor("localhost", wireMockServer.port());
    }

    @AfterEach
    public void teardown() {
        wireMockServer.stop();
    }

    @Test
    public void happyPath200(Vertx vertx, VertxTestContext testContext) throws Exception {
        int port = wireMockServer.port();
        JsonObject payload = new JsonObject("{ \"lorem ipsum\":\"boring text\" }");
        wireMockServer.stubFor(get(urlEqualTo("/my/resource"))
                .withHeader("Accept", matching("application/json"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(payload.encode())));

        String url = "http://localhost:" + port + "/my/resource";

        Engine engine = new Engine(
                ResourceUtils.fileContentsFromResource(
                        "com/redhat/vertx/pipeline/step/httpClientIntegrationTest.yaml"
                ));
        JsonObject doc = new JsonObject().put("url", url);
        vertx.rxDeployVerticle(engine).timeout(1, TimeUnit.SECONDS).blockingGet();
        JsonObject d2 = (JsonObject) engine.execute(doc).timeout(5, TimeUnit.SECONDS).blockingGet();

        assertThat(d2.containsKey("response")).isTrue();
        assertThat(d2.getJsonObject("response")).isEqualTo(payload);

        verify(getRequestedFor(urlMatching("/my/resource"))
                .withHeader("Accept", matching("application/json")));

        testContext.completeNow();
    }

    @Test
    @Disabled // TODO terminates event execution
    public void happyPath204(Vertx vertx, VertxTestContext testContext) throws Exception {
        int port = wireMockServer.port();
        wireMockServer.stubFor(get(urlEqualTo("/my/resource"))
                .withHeader("Accept", matching("application/json"))
                .willReturn(aResponse().withStatus(204)));

        String url = "http://localhost:" + port + "/my/resource";

        Engine engine = new Engine(
                ResourceUtils.fileContentsFromResource(
                        "com/redhat/vertx/pipeline/step/httpClientIntegrationTest.yaml"
                ));
        JsonObject doc = new JsonObject().put("url",url);
        vertx.rxDeployVerticle(engine).timeout(1, TimeUnit.SECONDS).blockingGet();
        AtomicReference<Disposable> docSub = new AtomicReference<>();
        DisposableHelper.set(docSub, engine.execute(doc).timeout(10, TimeUnit.SECONDS)
                .doOnError(testContext::failNow)
                .doOnSuccess(r -> validate204(r, testContext))
                .doAfterTerminate(() -> {
                    DisposableHelper.dispose(docSub);
                    testContext.completeNow();
                })
                .test()
                .assertSubscribed());

        assertThat(testContext.awaitCompletion(6, TimeUnit.SECONDS)).isTrue();
    }

    private void validate204(Object r, VertxTestContext testContext) {
        JsonObject jo = (JsonObject)r;
            assertThat(jo.containsKey("response")).isTrue(); // This could well be false because the result is empty
            assertThat(jo.getJsonObject("response")).isNull();
            verify(getRequestedFor(urlMatching("/my/resource"))
                    .withHeader("Accept", matching("application/json")));
            testContext.completeNow();
    }

    @Test
    public void serverError500(Vertx vertx, VertxTestContext testContext) throws Exception {
        Logger logger = Logger.getLogger(this.getClass().getName() + "#" + Thread.currentThread().getStackTrace()[0].getMethodName());
        int port = wireMockServer.port();
        JsonObject payload = new JsonObject("{ \"lorem ipsum\":\"boring text\" }");
        wireMockServer.stubFor(get(urlEqualTo("/my/resource"))
                .withHeader("Accept", matching("application/json"))
                .willReturn(aResponse()
                        .withStatus(500)
                        .withHeader("Content-Type", "text/html")
                        .withBody("<html><head/><body>500 Internal server error</body></html>")));

        String url = "http://localhost:" + port + "/my/resource";

        Engine engine = new Engine(
                ResourceUtils.fileContentsFromResource(
                        "com/redhat/vertx/pipeline/step/httpClientIntegrationTest.yaml"
                ));
        JsonObject doc = new JsonObject().put("url", url);
        vertx.rxDeployVerticle(engine).timeout(1, TimeUnit.SECONDS).blockingGet();

        AtomicReference<Disposable> docSub = new AtomicReference<>();

        DisposableHelper.set(docSub,
                engine.execute(doc)
                        .doOnError(t -> {
                            logger.info("Evaluating exception " + t.toString());
                            assertThat(t).isNotInstanceOf(HttpClient.HttpResponseStatusException.class);
                        })
                        .doAfterTerminate(() -> {
                            logger.info("Disposing after terminate");
                            DisposableHelper.dispose(docSub);
                            testContext.completeNow();
                        })
                        .test()
                        .assertSubscribed());

        Arrays.asList(logger.getHandlers()).forEach(Handler::flush);
        assertThat(testContext.awaitCompletion(6, TimeUnit.SECONDS)).isTrue();
        logger.info("Complete.");
    }

}

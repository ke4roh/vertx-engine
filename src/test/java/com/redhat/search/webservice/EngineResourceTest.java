package com.redhat.search.webservice;

import io.quarkus.test.junit.QuarkusTest;
import org.apache.http.entity.ContentType;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.hamcrest.CoreMatchers.is;

@Disabled
@QuarkusTest
public class EngineResourceTest {

    @Test
    public void testHelloEndpoint() {
        given()
          .when().body("{ \"q\":\"lorem ipsum\" }").contentType(APPLICATION_JSON).post("/query/hello-world-pipeline")
          .then()
             .statusCode(200)
             .body(is("Woohoo!pipe1{ \"q\":\"lorem ipsum\" }"));
    }

}
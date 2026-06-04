package nz.co.ksktech.congresstrades.api;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusIntegrationTest;
import nz.co.ksktech.congresstrades.testsupport.WireMockTestResource;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;

/**
 * Black-box integration test that exercises the <strong>packaged application</strong>
 * (the built jar) through HTTP, run by Failsafe in the {@code integration-test}
 * phase — complementing the in-JVM {@code @QuarkusTest}s run by Surefire.
 *
 * <p>{@code @QuarkusIntegrationTest} launches the artifact as a separate process,
 * so there is no CDI injection here; everything is asserted over REST. External
 * HTTP is still stubbed by {@link WireMockTestResource} (its config overrides are
 * propagated to the launched app), and the database comes from Dev Services.</p>
 *
 * <p>The digest endpoint is intentionally excluded — it needs a real LLM key,
 * which a black-box smoke test should not depend on.</p>
 */
@QuarkusIntegrationTest
@QuarkusTestResource(WireMockTestResource.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AppSmokeIT {

    @Test
    @Order(1)
    void health_readyIsUp() {
        given().when().get("/q/health/ready")
                .then().statusCode(200)
                .body("status", is("UP"));
    }

    @Test
    @Order(2)
    void ingest_thenTradesAndMembersArePresent() {
        given().when().post("/api/v1/admin/ingest")
                .then().statusCode(200)
                .body("tradesFetched", greaterThan(0));

        given().when().get("/api/v1/trades")
                .then().statusCode(200)
                .body("size()", greaterThan(0));

        given().when().get("/api/v1/members")
                .then().statusCode(200)
                .body("fullName", hasItem("Jane Representative"));
    }

    @Test
    @Order(3)
    void signals_detectAndList() {
        given().when().post("/api/v1/signals/detect")
                .then().statusCode(200);

        given().when().get("/api/v1/signals")
                .then().statusCode(200)
                .body("size()", greaterThan(0));
    }
}

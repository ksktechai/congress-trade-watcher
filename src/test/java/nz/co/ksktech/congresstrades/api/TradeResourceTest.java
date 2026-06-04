package nz.co.ksktech.congresstrades.api;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import nz.co.ksktech.congresstrades.testsupport.WireMockTestResource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

/**
 * End-to-end API tests over the full stack: Testcontainers Postgres (via Quarkus
 * Dev Services) + Flyway-managed schema + WireMock-stubbed external APIs. No real
 * Finnhub or Anthropic calls happen.
 */
@QuarkusTest
@QuarkusTestResource(WireMockTestResource.class)
class TradeResourceTest {

    @BeforeEach
    void ingestData() {
        // Idempotent — safe to run before every test to guarantee data exists.
        given().when().post("/api/v1/admin/ingest")
                .then().statusCode(200)
                .body("tradesFetched", greaterThan(0));
    }

    @Test
    void ingest_viaFinnhubSourceAlsoWorks() {
        given().queryParam("source", "finnhub")
                .when().post("/api/v1/admin/ingest")
                .then().statusCode(200)
                .body("tradesFetched", greaterThan(0));
    }

    @Test
    void listTrades_returnsIngestedTrades() {
        given().when().get("/api/v1/trades")
                .then().statusCode(200)
                .body("size()", greaterThan(0))
                .body("[0].ticker", notNullValue());
    }

    @Test
    void listTrades_filterByTicker() {
        given().queryParam("ticker", "AAPL")
                .when().get("/api/v1/trades")
                .then().statusCode(200)
                .body("ticker", everyItem(is("AAPL")));
    }

    @Test
    void listTrades_filterByType() {
        given().queryParam("type", "PURCHASE")
                .when().get("/api/v1/trades")
                .then().statusCode(200)
                .body("transactionType", everyItem(is("PURCHASE")));
    }

    @Test
    void members_areCreatedDuringIngestion() {
        given().when().get("/api/v1/members")
                .then().statusCode(200)
                .body("fullName", hasItem("Jane Representative"));
    }

    @Test
    void memberTrades_returnsThatMembersTrades() {
        given().pathParam("name", "Jane Representative")
                .when().get("/api/v1/members/{name}/trades")
                .then().statusCode(200)
                .body("size()", greaterThan(0))
                .body("memberName", everyItem(is("Jane Representative")));
    }

    @Test
    void memberTrades_unknownMemberReturns404() {
        given().pathParam("name", "Nobody Here")
                .when().get("/api/v1/members/{name}/trades")
                .then().statusCode(404)
                .body("status", is(404));
    }

    @Test
    void signals_detectAndList() {
        given().when().post("/api/v1/signals/detect")
                .then().statusCode(200);

        given().when().get("/api/v1/signals")
                .then().statusCode(200)
                .body("size()", greaterThan(0));
    }

    @Test
    void dailyDigest_returnsNarrativeAndDisclaimer() {
        given().when().get("/api/v1/digest/daily")
                .then().statusCode(200)
                .body("narrative", notNullValue())
                .body("disclaimer", notNullValue())
                .body("signals", notNullValue());
    }

    @Test
    void health_readyIsUp() {
        given().when().get("/q/health/ready")
                .then().statusCode(200)
                .body("status", is("UP"));
    }
}

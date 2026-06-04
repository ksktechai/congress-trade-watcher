package nz.co.ksktech.congresstrades.api;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import nz.co.ksktech.congresstrades.testsupport.WireMockTestResource;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;

/**
 * Exercises the Mutiny {@code Uni}/{@code Multi} endpoints.
 */
@QuarkusTest
@QuarkusTestResource(WireMockTestResource.class)
class ReactiveDemoResourceTest {

    @Test
    void streamTickers_emitsSeededWatchlistAsSse() {
        given().accept("text/event-stream")
                .when().get("/api/v1/reactive/tickers")
                .then().statusCode(200)
                .body(containsString("AAPL"))
                .body(containsString("MSFT"));
    }

    @Test
    void reactiveTrades_returnsUniListFilteredByTicker() {
        // Seed data via the normal ingest path (WireMock-stubbed congress source).
        given().when().post("/api/v1/admin/ingest").then().statusCode(200);

        given().queryParam("ticker", "AAPL")
                .when().get("/api/v1/reactive/trades")
                .then().statusCode(200)
                .body("size()", greaterThan(0))
                .body("ticker", everyItem(is("AAPL")));
    }
}

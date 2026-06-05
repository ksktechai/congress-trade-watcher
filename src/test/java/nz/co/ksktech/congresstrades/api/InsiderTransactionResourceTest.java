package nz.co.ksktech.congresstrades.api;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import nz.co.ksktech.congresstrades.testsupport.WireMockTestResource;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

/**
 * Insider-transactions endpoint, with Finnhub stubbed by WireMock.
 */
@QuarkusTest
@QuarkusTestResource(WireMockTestResource.class)
class InsiderTransactionResourceTest {

    @Test
    void insiderTransactions_returnsDataForSymbol() {
        given().queryParam("symbol", "tsla")
                .when().get("/api/v1/insider-transactions")
                .then().statusCode(200)
                .body("symbol", is("TSLA"))
                .body("count", greaterThan(0))
                .body("transactions.size()", greaterThan(0))
                .body("transactions.name", everyItem(notNullValue()))
                .body("transactions[0].transactionCode", notNullValue())
                .body("disclaimer", notNullValue());
    }

    @Test
    void insiderTransactions_symbolIsRequired() {
        given().when().get("/api/v1/insider-transactions")
                .then().statusCode(400)
                .body("status", is(400));
    }
}

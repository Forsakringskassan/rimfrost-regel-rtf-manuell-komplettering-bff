package se.fk.github.rtfmanuellkompletteringbff;

import io.quarkus.test.InjectMock;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import se.fk.rimfrost.framework.bff.ilogg.ILogg;
import se.fk.rimfrost.framework.bff.ilogg.exception.ILoggException;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static se.fk.github.rtfmanuellkompletteringbff.RtfManuellKompletteringBffControllerTest.GET_BODY;
import static se.fk.github.rtfmanuellkompletteringbff.RtfManuellKompletteringBffControllerTest.TEST_ID;

/**
 * Tests RTFKB-FR-05.5: publish failure must return error without exposing data.
 */
@QuarkusTest
@QuarkusTestResource(WireMockTestResource.class)
class InformationsaccessPublishFailureTest
{
   @InjectMock
   ILogg iLogg;

   @BeforeEach
   void setUp()
   {
      WireMockTestResource.getServer().resetAll();
   }

   @Test
   @DisplayName("RTFKB-FR-05.5: Om eventet inte kan publiceras returneras ett fel utan kompletteringsdata")
   void getKomplettering_returnsErrorWithoutData_whenEventCannotBePublished() throws ILoggException
   {
      WireMockTestResource.getServer().stubFor(get(urlEqualTo("/" + TEST_ID))
            .willReturn(aResponse()
                  .withHeader("Content-Type", "application/json")
                  .withBody(GET_BODY)));
      Mockito.doThrow(new ILoggException("Kafka unavailable", new RuntimeException("nack")))
            .when(iLogg).publish(Mockito.any());

      given()
            .when()
            .get("/api/" + TEST_ID + "/komplettering")
            .then()
            .statusCode(500)
            .body("error", equalTo("Internal server error"))
            .body("personnummer", nullValue());
   }
}

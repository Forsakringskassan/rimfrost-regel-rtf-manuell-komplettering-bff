package se.fk.github.rtfmanuellkompletteringbff;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.http.Fault;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;

@QuarkusTest
@QuarkusTestResource(WireMockTestResource.class)
class RtfManuellKompletteringBffControllerTest
{

   private static final String TEST_ID = "550e8400-e29b-41d4-a716-446655440000";

   private static final String VALID_BODY = """
         {
           "personnummer": "19800101-1234",
           "avsikt": "Ansöka om VAB"
         }
         """;

   @BeforeEach
   void setUp()
   {
      WireMockTestResource.getServer().resetAll();
   }

   @Test
   void getKomplettering_returnsBackendData()
   {
      WireMockTestResource.getServer().stubFor(get(urlEqualTo("/" + TEST_ID))
            .willReturn(aResponse()
                  .withHeader("Content-Type", "application/json")
                  .withBody("""
                        {
                          "personnummer": "19800101-1234",
                          "avsikt": "Ansöka om VAB"
                        }
                        """)));

      given()
            .header("Authorization", "Bearer test-token")
            .when()
            .get("/api/" + TEST_ID + "/komplettering")
            .then()
            .statusCode(200)
            .body("personnummer", equalTo("19800101-1234"))
            .body("avsikt", equalTo("Ansöka om VAB"));
   }

   @Test
   void getKomplettering_passesAuthorizationHeaderThrough()
   {
      WireMockTestResource.getServer().stubFor(get(urlEqualTo("/" + TEST_ID))
            .withHeader("Authorization", WireMock.equalTo("Bearer test-token"))
            .willReturn(aResponse()
                  .withHeader("Content-Type", "application/json")
                  .withBody("{\"personnummer\": null, \"avsikt\": null}")));

      given()
            .header("Authorization", "Bearer test-token")
            .when()
            .get("/api/" + TEST_ID + "/komplettering")
            .then()
            .statusCode(200)
            .body("personnummer", nullValue());
   }

   @Test
   void getKomplettering_returns404_whenHandlaggningIsMissing()
   {
      WireMockTestResource.getServer().stubFor(get(urlEqualTo("/" + TEST_ID))
            .willReturn(aResponse().withStatus(404)));

      given()
            .header("Authorization", "Bearer test-token")
            .when()
            .get("/api/" + TEST_ID + "/komplettering")
            .then()
            .statusCode(404)
            .body("error", equalTo("Upstream error"));
   }

   @Test
   void getKomplettering_returns502_whenBackendUnreachable()
   {
      WireMockTestResource.getServer().stubFor(get(urlEqualTo("/" + TEST_ID))
            .willReturn(aResponse().withFault(Fault.CONNECTION_RESET_BY_PEER)));

      given()
            .header("Authorization", "Bearer test-token")
            .when()
            .get("/api/" + TEST_ID + "/komplettering")
            .then()
            .statusCode(502)
            .body("error", equalTo("Upstream unavailable"));
   }

   @Test
   void patchKomplettering_returns204_onSuccess()
   {
      WireMockTestResource.getServer().stubFor(patch(urlEqualTo("/" + TEST_ID))
            .withRequestBody(equalToJson(VALID_BODY))
            .willReturn(aResponse().withStatus(204)));

      given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer test-token")
            .body(VALID_BODY)
            .when()
            .patch("/api/" + TEST_ID + "/komplettering")
            .then()
            .statusCode(204);
   }

   @Test
   void patchKomplettering_returns400_whenPersonnummerIsMissing()
   {
      given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer test-token")
            .body("{\"avsikt\": \"Ansöka om VAB\"}")
            .when()
            .patch("/api/" + TEST_ID + "/komplettering")
            .then()
            .statusCode(400);
   }

   // The generated RtfKompletteringData carries @NotNull, not @NotBlank, so a blank value passes
   // validation and reaches the rule service. There it counts as still missing, and the handlaggare
   // first learns of it when /done answers 422. Pinning that here so it changes visibly if the
   // OpenAPI spec ever gains minLength: 1 — see docs/teknisk-spec.md.
   @Test
   void patchKomplettering_forwardsBlankAvsikt()
   {
      WireMockTestResource.getServer().stubFor(patch(urlEqualTo("/" + TEST_ID))
            .willReturn(aResponse().withStatus(204)));

      given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer test-token")
            .body("{\"personnummer\": \"19800101-1234\", \"avsikt\": \"  \"}")
            .when()
            .patch("/api/" + TEST_ID + "/komplettering")
            .then()
            .statusCode(204);
   }

   @Test
   void patchKomplettering_returns503_whenBackendIsUnavailable()
   {
      WireMockTestResource.getServer().stubFor(patch(urlEqualTo("/" + TEST_ID))
            .willReturn(aResponse().withStatus(503)));

      given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer test-token")
            .body(VALID_BODY)
            .when()
            .patch("/api/" + TEST_ID + "/komplettering")
            .then()
            .statusCode(503)
            .body("error", equalTo("Upstream error"));
   }

   @Test
   void kompletteringDone_returns204_onSuccess()
   {
      WireMockTestResource.getServer().stubFor(post(urlEqualTo("/" + TEST_ID + "/done"))
            .willReturn(aResponse().withStatus(204)));

      given()
            .header("Authorization", "Bearer test-token")
            .when()
            .post("/api/" + TEST_ID + "/komplettering/done")
            .then()
            .statusCode(204);
   }

   @Test
   void kompletteringDone_returns422_whenYrkandeIsStillIncomplete()
   {
      WireMockTestResource.getServer().stubFor(post(urlEqualTo("/" + TEST_ID + "/done"))
            .willReturn(aResponse().withStatus(422)));

      given()
            .header("Authorization", "Bearer test-token")
            .when()
            .post("/api/" + TEST_ID + "/komplettering/done")
            .then()
            .statusCode(422)
            .body("error", equalTo("Upstream error"));
   }

   @Test
   void kompletteringDone_returns409_whenCorrelationStateIsCleared()
   {
      WireMockTestResource.getServer().stubFor(post(urlEqualTo("/" + TEST_ID + "/done"))
            .willReturn(aResponse().withStatus(409)));

      given()
            .header("Authorization", "Bearer test-token")
            .when()
            .post("/api/" + TEST_ID + "/komplettering/done")
            .then()
            .statusCode(409)
            .body("error", equalTo("Upstream error"));
   }

   @Test
   void getUppgiftsbeskrivning_returnsData()
   {
      WireMockTestResource.getServer().stubFor(get(urlEqualTo("/utokadUppgiftsbeskrivning"))
            .willReturn(aResponse()
                  .withHeader("Content-Type", "application/json")
                  .withBody("{\"beskrivning\": \"Komplettera med saknade uppgifter\"}")));

      given()
            .when()
            .get("/api/uppgiftsbeskrivning/RTF_MANUELL_KOMPLETTERING")
            .then()
            .statusCode(200)
            .body("beskrivning", equalTo("Komplettera med saknade uppgifter"));
   }

   @Test
   void getUppgiftsbeskrivning_returns503_whenBackendReturnsError()
   {
      WireMockTestResource.getServer().stubFor(get(urlEqualTo("/utokadUppgiftsbeskrivning"))
            .willReturn(aResponse().withStatus(503)));

      given()
            .when()
            .get("/api/uppgiftsbeskrivning/RTF_MANUELL_KOMPLETTERING")
            .then()
            .statusCode(503)
            .body("error", equalTo("Upstream error"));
   }
}

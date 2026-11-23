package se.fk.github.rtfmanuellkompletteringbff;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.http.Fault;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.smallrye.reactive.messaging.memory.InMemoryConnector;
import jakarta.inject.Inject;
import org.eclipse.microprofile.reactive.messaging.spi.Connector;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import se.fk.rimfrost.informationsaccess.InformationsaccessEvent;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
@QuarkusTestResource(WireMockTestResource.class)
class RtfManuellKompletteringBffControllerTest
{

   static final String TEST_ID = "550e8400-e29b-41d4-a716-446655440000";

   static final String UPPGIFT_ID = "6f1c2d3e-4b5a-4c6d-8e7f-9a0b1c2d3e4f";

   static final String REGEL_ID = "7a8b9c0d-1e2f-4a3b-8c4d-5e6f7a8b9c0d";

   static final String ANVANDARE_TYP_ID = "c3d4e5f6-a7b8-4c9d-8e0f-1a2b3c4d5e6f";

   /** Regeltjänstens GET-svar: data plus informationsaccess-wrapper (RTFKB-FR-05). */
   static String backendGetBody(String data)
   {
      return """
            {
              "data": %s,
              "informationsaccess": {
                "uppgiftId": "%s",
                "regelId": "%s",
                "regelVersion": "1",
                "anvandare": { "typId": "%s", "varde": "H123456" }
              }
            }
            """.formatted(data, UPPGIFT_ID, REGEL_ID, ANVANDARE_TYP_ID);
   }

   static final String GET_BODY = backendGetBody("""
         { "personnummer": "19800101-1234", "avsikt": "Ansöka om VAB" }
         """);

   private static final String VALID_BODY = """
         {
           "personnummer": "19800101-1234",
           "avsikt": "Ansöka om VAB"
         }
         """;

   @Inject
   @Connector("smallrye-in-memory")
   InMemoryConnector inMemoryConnector;

   @BeforeEach
   void setUp()
   {
      WireMockTestResource.getServer().resetAll();
      inMemoryConnector.sink("ilogg-event").clear();
   }

   @Test
   @DisplayName("RTFKB-FR-05.3: Kompletteringsdata packas upp ur regeltjänstens svar")
   void getKomplettering_returnsBackendData()
   {
      WireMockTestResource.getServer().stubFor(get(urlEqualTo("/" + TEST_ID))
            .willReturn(aResponse()
                  .withHeader("Content-Type", "application/json")
                  .withBody(GET_BODY)));

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
   @DisplayName("RTFKB-NFR-02.1, RTFKB-FR-01.2: Authorization skickas vidare och tomma fält returneras")
   void getKomplettering_passesAuthorizationHeaderThrough()
   {
      WireMockTestResource.getServer().stubFor(get(urlEqualTo("/" + TEST_ID))
            .withHeader("Authorization", WireMock.equalTo("Bearer test-token"))
            .willReturn(aResponse()
                  .withHeader("Content-Type", "application/json")
                  .withBody(backendGetBody("{\"personnummer\": null, \"avsikt\": null}"))));

      given()
            .header("Authorization", "Bearer test-token")
            .when()
            .get("/api/" + TEST_ID + "/komplettering")
            .then()
            .statusCode(200)
            .body("personnummer", nullValue());
   }

   @Test
   @DisplayName("RTFKB-FR-05.1, RTFKB-FR-05.2: Hämtning av kompletteringsdata publicerar ett informationsaccess-event med förväntat innehåll")
   void getKomplettering_publishesInformationsaccessEvent()
   {
      stubGetKomplettering();

      given()
            .header("Authorization", "Bearer test-token")
            .when()
            .get("/api/" + TEST_ID + "/komplettering")
            .then()
            .statusCode(200);

      var received = inMemoryConnector.sink("ilogg-event").received();
      assertEquals(1, received.size());
      var event = (InformationsaccessEvent) received.getFirst().getPayload();
      assertNotNull(event.getId());
      assertEquals(ANVANDARE_TYP_ID, event.getAnvandare().getTypId());
      assertEquals("H123456", event.getAnvandare().getVarde());
      assertNotNull(event.getTidpunkt());
      assertEquals("rimfrost-regel-rtf-manuell-komplettering-bff", event.getKalla());
      assertEquals(TEST_ID, event.getHandlaggningId());
      assertEquals(UPPGIFT_ID, event.getUppgiftId());
      assertEquals(REGEL_ID, event.getRegelId());
      assertEquals("1", event.getRegelVersion());
      assertEquals("19800101-1234", event.getVisadInformation().get("personnummer"));
      assertEquals("Ansöka om VAB", event.getVisadInformation().get("avsikt"));
   }

   @Test
   @DisplayName("RTFKB-FR-05.3: Informationsaccessdata skickas inte vidare till frontend")
   void getKomplettering_doesNotForwardInformationsaccessToFrontend()
   {
      stubGetKomplettering();

      given()
            .header("Authorization", "Bearer test-token")
            .when()
            .get("/api/" + TEST_ID + "/komplettering")
            .then()
            .statusCode(200)
            .body("informationsaccess", nullValue())
            .body("data", nullValue())
            .body("personnummer", equalTo("19800101-1234"));
   }

   @Test
   @DisplayName("RTFKB-FR-05.4: Misslyckad hämtning ger inget informationsaccess-event")
   void getKomplettering_publishesNoEvent_whenBackendFails()
   {
      WireMockTestResource.getServer().stubFor(get(urlEqualTo("/" + TEST_ID))
            .willReturn(aResponse().withStatus(404)));

      given().when().get("/api/" + TEST_ID + "/komplettering").then().statusCode(404);

      assertTrue(inMemoryConnector.sink("ilogg-event").received().isEmpty());
   }

   @Test
   @DisplayName("RTFKB-FR-05.4: Registrering, slutförande och uppgiftsbeskrivning ger inget informationsaccess-event")
   void otherEndpoints_publishNoEvent()
   {
      WireMockTestResource.getServer().stubFor(patch(urlEqualTo("/" + TEST_ID))
            .willReturn(aResponse().withStatus(204)));
      WireMockTestResource.getServer().stubFor(post(urlEqualTo("/" + TEST_ID + "/done"))
            .willReturn(aResponse().withStatus(204)));
      WireMockTestResource.getServer().stubFor(get(urlEqualTo("/utokadUppgiftsbeskrivning"))
            .willReturn(aResponse()
                  .withHeader("Content-Type", "application/json")
                  .withBody("{\"beskrivning\": \"Komplettera\"}")));

      given().contentType(ContentType.JSON).body(VALID_BODY)
            .when().patch("/api/" + TEST_ID + "/komplettering").then().statusCode(204);
      given().when().post("/api/" + TEST_ID + "/komplettering/done").then().statusCode(204);
      given().when().get("/api/uppgiftsbeskrivning/RTF_MANUELL_KOMPLETTERING").then().statusCode(200);

      assertTrue(inMemoryConnector.sink("ilogg-event").received().isEmpty());
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

   @Test
   void patchKomplettering_returns400_whenAvsiktIsMissing()
   {
      given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer test-token")
            .body("{\"personnummer\": \"910101-1234\"}")
            .when()
            .patch("/api/" + TEST_ID + "/komplettering")
            .then()
            .statusCode(400);
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

   @Test
   @DisplayName("RTFKB-NFR-02.1: Authorization skickas vidare vid registrering och slutförande")
   void patchAndDone_forwardAuthorizationHeader()
   {
      WireMockTestResource.getServer().stubFor(patch(urlEqualTo("/" + TEST_ID))
            .withHeader("Authorization", WireMock.equalTo("Bearer test-token"))
            .willReturn(aResponse().withStatus(204)));
      WireMockTestResource.getServer().stubFor(post(urlEqualTo("/" + TEST_ID + "/done"))
            .withHeader("Authorization", WireMock.equalTo("Bearer test-token"))
            .willReturn(aResponse().withStatus(204)));

      given().contentType(ContentType.JSON).header("Authorization", "Bearer test-token").body(VALID_BODY)
            .when().patch("/api/" + TEST_ID + "/komplettering").then().statusCode(204);
      given().header("Authorization", "Bearer test-token")
            .when().post("/api/" + TEST_ID + "/komplettering/done").then().statusCode(204);
   }

   private static void stubGetKomplettering()
   {
      WireMockTestResource.getServer().stubFor(get(urlEqualTo("/" + TEST_ID))
            .willReturn(aResponse()
                  .withHeader("Content-Type", "application/json")
                  .withBody(GET_BODY)));
   }
}

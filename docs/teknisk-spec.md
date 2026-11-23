# Teknisk spec — RTF Manuell Komplettering BFF (RTFKB)

## Översikt

Tunn, synkron Quarkus REST-BFF utan egen datalagring. Uppströmsberoendet är regeltjänsten
`rimfrost-regel-rtf-manuell-komplettering`, nådd via en REST-klient vars typer kommer från
regeltjänstens och det gemensamma OUL-ramverkets OpenAPI-specifikationer. Vid hämtning av
kompletteringsdata publiceras ett informationsaccess-event till ILogg via Kafka (RTFKB-FR-05).

BFF:n bygger på `rimfrost-framework-bff`, som står för felmappning (`GlobalExceptionMapper`),
vidarebefordran av `Authorization`-headern och ILogg-klienten.

## Komponentstruktur

```text
src/main/java/se/fk/github/rtfmanuellkompletteringbff
├── RtfManuellKompletteringBffController               # REST-ändpunkter mot frontend
├── LoggingFilterNpeMapper                             # 502 vid avbruten anslutning (se nedan)
├── integration/RtfManuellKompletteringClient          # REST-klient mot regeltjänsten
├── integration/InformationsaccessPublisher            # Bygger och publicerar informationsaccess-event
└── model/BackendGetResponse, BackendInformationsaccess, BackendAnvandare
                                                       # Regeltjänstens GET-svar {data, informationsaccess}
```

Mot frontend använder BFF:n de genererade typerna från regeltjänstens och OUL-ramverkets
OpenAPI-specifikationer, så BFF:ns kontrakt mot frontend kan inte glida ifrån regeltjänstens.
De enda egna modelltyperna beskriver kompletteringsramverkets omslag kring GET-svaret
(`RegelKompletteringReadResponse` i `rimfrost-framework-regel-komplettering`), som inte finns i
regeltjänstens OpenAPI-specifikation.

## API-specifikationer

| API | Specifikationsartefakt | Basväg |
|---|---|---|
| RTF Manuell Komplettering-regeltjänsten | `rimfrost-regel-rtf-manuell-komplettering-openapi` (`RtfKompletteringData`) | Konfigurerad via `BACKEND_URL` |
| Gemensamma OUL-ändpunkter | `rimfrost-framework-regel-oul-openapi` (`GetUtokadUppgiftsbeskrivningResponse`) | Samma bas-URL |

| Metod | Sökväg | Bakomliggande ändpunkt | Beskrivning |
|---|---|---|---|
| GET | `/api/{handlaggningId}/komplettering` | `GET /{handlaggningId}` | Hämta aktuell kompletteringsdata (`data` ur svaret), publicera informationsaccess-event |
| PATCH | `/api/{handlaggningId}/komplettering` | `PATCH /{handlaggningId}` | Registrera personnummer och avsikt |
| POST | `/api/{handlaggningId}/komplettering/done` | `POST /{handlaggningId}/done` | Slutför komplettering, avslutar OUL-uppgiften |
| GET | `/api/uppgiftsbeskrivning/{uppgiftstyp}` | `GET /utokadUppgiftsbeskrivning` | Hämta hjälptext |

### Informationsaccess vid hämtning

Regeltjänsten svarar på `GET /{handlaggningId}` med `{"data": RtfKompletteringData,
"informationsaccess": {uppgiftId, regelId, regelVersion, anvandare}}`. BFF:n packar upp
`data`, bygger ett `InformationsaccessEvent` med `data` som `visadInformation` och publicerar
det via `ILogg` innan svaret skickas. Bara `data` når frontend. Om publiceringen misslyckas
kastas `InformationsaccessPublishException`, som ramverkets `GlobalExceptionMapper` svarar 500
på, så ingen data visas utan att åtkomsten loggats. Mönstret är detsamma som i
`rimfrost-regel-bekraftabeslut-bff` (BEKB-FR-07).

### Validering av registrerade uppgifter

`PATCH` tar emot den genererade `RtfKompletteringData` direkt och validerar den med `@Valid`.
Den genererade typen bär `@NotNull` på båda fälten, så ett utelämnat `personnummer` eller
`avsikt` avvisas med 400. Det skyddet behövs: regeltjänstens `registerSvar` skriver alltid om
båda fälten på yrkandet utifrån begäran, så ett utelämnat fält skulle radera redan registrerad
data snarare än att lämna den orörd.

Ett tomt eller blanktecken-värde passerar däremot valideringen, eftersom specen inte anger
`minLength`. Regeltjänsten räknar ett sådant värde som fortfarande saknat, så `PATCH` svarar
204 medan `done` senare svarar 422. Se kända begränsningar nedan.

### Statuskoder från slutförandet

Regeltjänstens 409 (korrelationstillståndet redan tömt av timeout) och 422 (yrkandet
fortfarande ofullständigt) är båda meningsfulla utfall för frontend och ska nå fram med
bibehållen statuskod. Det kräver ingen särskild hantering i kontrollern: REST-klienten kastar
`ClientWebApplicationException` för statuskoder över 400, och ramverkets `GlobalExceptionMapper`
vidarebefordrar statuskoden som den är. Kontrollern behöver därför bara hantera lyckat utfall,
och regeltjänstens framgångssvar är alltid exakt 204.

### Avbruten anslutning mot regeltjänsten

Vid en återställd anslutning saknar svaret headers, och `LoggingContextClientResponseFilter`
i `fk-logging` läser dem utan nullkontroll. Det resulterande `NullPointerException` bär inget
`IOException` någonstans i sin kedja, och ramverkets `GlobalExceptionMapper` svarar därför 500.
`LoggingFilterNpeMapper` mappar `NullPointerException` (mer specifik än ramverkets
`Exception`-hanterare), känner igen just den NPE:n på den stackram den kastats från och svarar
502. Andra NPE:er lämnas tillbaka till ramverkets hanterare och får samma svar som där. Skulle `fk-logging` framöver nullskydda
anropet, eller ramverket själv känna igen fallet, kan klassen tas bort.

## Kafka-integration

| Kanal | Riktning | Topic | Innehåll |
|---|---|---|---|
| `ilogg-event` | Utgående | `informationsaccess` | `InformationsaccessEvent` (från `rimfrost-service-informationsaccess-asyncapi`) |

Kafka-adressen sätts med `MP_MESSAGING_CONNECTOR_SMALLRYE_KAFKA_BOOTSTRAP_SERVERS`. Utan den
försöker BFF:n nå `localhost:9092`, och GET svarar 500 efter publiceringens timeout.

## Konfiguration

| Egenskap | Beskrivning | Standardvärde |
|---|---|---|
| `quarkus.rest-client.backend.url` (`BACKEND_URL`) | Bas-URL till regeltjänsten | `http://localhost:8080/regel/rtf-manuell-komplettering` |
| `CORS_ORIGINS` | Tillåtna ursprung för CORS | Lokala mikrofrontend-portar |
| `quarkus.http.port` | Lyssnarport | `9004` |
| `MP_MESSAGING_CONNECTOR_SMALLRYE_KAFKA_BOOTSTRAP_SERVERS` | Kafka-adress för informationsaccess-event | `localhost:9092` |
| `rimfrost.ilogg.publish-timeout` | Max väntetid på att eventet publiceras | `10s` |

## Liveness

`/q/health`.

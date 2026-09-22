# Teknisk spec — RTF Manuell Komplettering BFF (RTFKB)

## Översikt

Tunn, synkron Quarkus REST-BFF utan egen datalagring och utan meddelandeintegration. Enda
uppströmsberoende är regeltjänsten `rimfrost-regel-rtf-manuell-komplettering`, nådd via en
REST-klient vars typer kommer från regeltjänstens och det gemensamma OUL-ramverkets
OpenAPI-specifikationer.

## Komponentstruktur

```text
src/main/java/se/fk/github/rtfmanuellkompletteringbff
├── RtfManuellKompletteringBffController      # REST-ändpunkter mot frontend
├── integration/RtfManuellKompletteringClient # REST-klient mot regeltjänsten
└── GlobalExceptionMapper                     # Enhetlig felmappning, inkl. hantering av avbrutna anslutningar
```

BFF:n har inga egna modelltyper. Både begäran och svar använder de genererade typerna från
regeltjänstens och OUL-ramverkets OpenAPI-specifikationer, så BFF:ns kontrakt mot frontend
kan inte glida ifrån regeltjänstens.

## API-specifikationer

| API | Specifikationsartefakt | Basväg |
|---|---|---|
| RTF Manuell Komplettering-regeltjänsten | `rimfrost-regel-rtf-manuell-komplettering-openapi` (`RtfKompletteringData`) | Konfigurerad via `BACKEND_URL` |
| Gemensamma OUL-ändpunkter | `rimfrost-framework-regel-oul-openapi` (`GetUtokadUppgiftsbeskrivningResponse`) | Samma bas-URL |

| Metod | Sökväg | Bakomliggande ändpunkt | Beskrivning |
|---|---|---|---|
| GET | `/api/{handlaggningId}/komplettering` | `GET /{handlaggningId}` | Hämta aktuell kompletteringsdata |
| PATCH | `/api/{handlaggningId}/komplettering` | `PATCH /{handlaggningId}` | Registrera personnummer och avsikt |
| POST | `/api/{handlaggningId}/komplettering/done` | `POST /{handlaggningId}/done` | Slutför komplettering, avslutar OUL-uppgiften |
| GET | `/api/uppgiftsbeskrivning/{uppgiftstyp}` | `GET /utokadUppgiftsbeskrivning` | Hämta hjälptext |

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
`ClientWebApplicationException` för statuskoder över 400, och `GlobalExceptionMapper`
vidarebefordrar statuskoden som den är. Kontrollern behöver därför bara hantera lyckat utfall,
och regeltjänstens framgångssvar är alltid exakt 204.

### Avbruten anslutning mot regeltjänsten

Vid en återställd anslutning saknar svaret headers, och `LoggingContextClientResponseFilter`
i `fk-logging` läser dem utan nullkontroll. Det resulterande `NullPointerException` bär inget
`IOException` någonstans i sin kedja, så `GlobalExceptionMapper` känner även igen just den
NPE:n på den stackram den kastats från och svarar 502. Skulle `fk-logging` framöver
nullskydda anropet slutar mönstret matcha, och felet passerar i stället den ordinarie
`ProcessingException`-vägen — som också svarar 502.

## Kafka-integration

Ingen. Tjänsten har ingen meddelandeintegration.

## Konfiguration

| Egenskap | Beskrivning | Standardvärde |
|---|---|---|
| `quarkus.rest-client.backend.url` (`BACKEND_URL`) | Bas-URL till regeltjänsten | `http://localhost:8080/regel/rtf-manuell-komplettering` |
| `CORS_ORIGINS` | Tillåtna ursprung för CORS | Lokala mikrofrontend-portar |
| `quarkus.http.port` | Lyssnarport | `9004` |

## Liveness

`/q/health`.

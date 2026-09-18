# Krav — RTF Manuell Komplettering BFF (RTFKB)

## Bakgrund och syfte

RTF Manuell Komplettering BFF är backend-för-frontend för mikrofrontenden som hanterar
komplettering av ett ofullständigt yrkande inför en manuell RTF-kontroll. Den förmedlar
handläggarens hämtning av redan registrerade uppgifter, registrering av sökandens
kompletterande uppgifter, och slutförande av kompletteringen, mot den bakomliggande
regeltjänsten `rimfrost-regel-rtf-manuell-komplettering`. Den existerar för att mikrofrontenden
ska kunna anropa en enda, stabil tjänst utan kännedom om regeltjänstens kontrakt eller de
generiska ramverk den bygger på.

---

## Intressenter och aktörer

| Aktör | Roll |
|---|---|
| RTF Manuell Komplettering Frontend | Anropar BFF:n för att hämta och registrera kompletterande uppgifter |
| RTF Manuell Komplettering-regeltjänsten | Bakomliggande tjänst som äger handläggningens yrkandedata |

---

## Funktionella krav

### RTFKB-FR-01 — Hämta kompletteringsunderlag

- **RTFKB-FR-01.1** BFF:n ska kunna hämta aktuell kompletteringsdata för en given handläggning
  från bakomliggande regeltjänst och returnera den oförändrad till frontend.
- **RTFKB-FR-01.2** Kompletteringsdata ska kunna returneras även när fälten saknar värde,
  eftersom avsaknaden av värde är själva anledningen till att komplettering krävs.
- **RTFKB-FR-01.3** BFF:n ska kunna hämta en hjälptext för en uppgiftstyp, oavsett vilken
  uppgiftstyp som efterfrågas, eftersom bakomliggande tjänst i dagsläget tillhandahåller en
  och samma generella beskrivning.

### RTFKB-FR-02 — Registrera kompletterande uppgifter

- **RTFKB-FR-02.1** BFF:n ska ta emot både personnummer och avsikt från frontend och validera
  att båda är angivna innan vidare behandling, enligt regeltjänstens OpenAPI-kontrakt.
- **RTFKB-FR-02.2** BFF:n ska skicka de registrerade uppgifterna till bakomliggande regeltjänst.
- **RTFKB-FR-02.3** Registrering och slutförande ska vara separata anrop, så att frontend kan
  spara uppgifter utan att samtidigt slutföra kompletteringen.

### RTFKB-FR-03 — Slutföra komplettering

- **RTFKB-FR-03.1** BFF:n ska kunna begära att bakomliggande regeltjänst slutför
  kompletteringen och avslutar den tillhörande OUL-uppgiften.
- **RTFKB-FR-03.2** Utfallet av slutförandet ska förmedlas till frontend med bibehållen
  statuskod, så att frontend kan skilja på slutförd komplettering, fortfarande ofullständigt
  yrkande, och redan utgången korrelationstid.

### RTFKB-FR-04 — Felhantering vid integration mot regeltjänsten

- **RTFKB-FR-04.1** Om regeltjänsten svarar med ett felstatus ska BFF:n returnera samma
  HTTP-statuskod till frontend.
- **RTFKB-FR-04.2** Om regeltjänsten inte går att nå, inklusive vid avbrutna
  nätverksanslutningar, ska BFF:n returnera en statuskod som tydligt anger att bakomliggande
  tjänst är otillgänglig.
- **RTFKB-FR-04.3** Felsvar från BFF:n ska ha ett enhetligt format oavsett vilken ändpunkt som
  anropats.

---

## Icke-funktionella krav

### RTFKB-NFR-01 — Observerbarhet

- **RTFKB-NFR-01.1** BFF:n ska exponera en hälsokontroll för sin egen driftstatus.

### RTFKB-NFR-02 — Säkerhet

- **RTFKB-NFR-02.1** BFF:n ska vidarebefordra anropande handläggares auktoriseringsuppgifter
  till bakomliggande regeltjänst oförändrade.

---

## API-gränssnitt (översikt)

| API | Målgrupp | Specifikationsartefakt |
|---|---|---|
| RTF Manuell Komplettering BFF REST-API | RTF Manuell Komplettering Frontend | Ingen dedikerad BFF-specifikation |
| RTF Manuell Komplettering-regeltjänstens API | Denna BFF | `rimfrost-regel-rtf-manuell-komplettering-openapi` |
| Gemensamma OUL-ändpunkter | Denna BFF | `rimfrost-framework-regel-oul-openapi` |

---

## Integration med RTF Manuell Komplettering-regeltjänsten

BFF:n är en ren synkron REST-till-REST-integration mot regeltjänsten, som i sin tur bygger på
det generiska kompletteringsramverket. BFF:n lagrar inget tillstånd själv.

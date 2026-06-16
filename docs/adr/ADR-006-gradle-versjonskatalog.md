# ADR-006: Gradle versjonskatalog for avhengigheter og plugins

**Dato:** 2026-06-16  
**Status:** Godkjent  
**Beslutningstakere:** Team Sikkerhetstjenesten

## Kontekst

`build.gradle.kts` inneholdt tidligere mange hardkodede versjoner for både plugins og avhengigheter. Dette gjorde bygget vanskeligere å vedlikeholde over tid:

- Versjoner var spredt i samme fil som build-logikken
- Oppgraderinger krevde manuelle søk i Kotlin DSL-scriptet
- Det var vanskeligere å se hvilke biblioteker som faktisk var styrende for bygget
- Duplisering og navngivning i testavhengigheter gjorde scriptet mindre lesbart

Prosjektet bruker Gradle med Kotlin DSL, og behovet var å gjøre byggkonfigurasjonen mer deklarativ og enklere å vedlikeholde uten å endre runtime-adferd.

## Beslutning

Vi bruker Gradle version catalog i `gradle/libs.versions.toml` som primær kilde for:

- plugin-versjoner
- biblioteksversjoner
- biblioteksaliaser
- bundles for relaterte avhengigheter, spesielt testavhengigheter

`build.gradle.kts` skal deretter bruke katalogen via `libs`-aliaser for å gjøre dependency- og plugin-definisjoner kortere og mer lesbare.

### Omfang

Følgende er flyttet til versjonskatalogen:

- Kotlin-pluginversjon
- Spring Boot- og Gradle-pluginversjoner
- observability-, logging- og sikkerhetsavhengigheter
- testbiblioteker og test-bundles

### Avgrensning

Vi lar enkelte byggkonstanter fortsatt stå eksplisitt i `build.gradle.kts` når det gir bedre stabilitet eller lesbarhet i Kotlin DSL-editoren. Version catalog brukes derfor primært for plugins og avhengigheter, ikke nødvendigvis for alle top-level buildkonstanter.

## Alternativer vurdert

### Alternativ A: Beholde versjoner i `build.gradle.kts`
- **Fordeler:** Ingen ny fil, alt ligger ett sted
- **Ulemper:** Dårligere oversikt, mer støy i build-scriptet, vanskeligere oppgraderinger
- **Vurdering:** Skalerer dårlig når antall avhengigheter øker

### Alternativ B: Gradle version catalog i TOML (valgt)
- **Fordeler:** Tydelig separasjon mellom versjonsdata og buildlogikk, bedre lesbarhet, enklere oppgraderinger
- **Ulemper:** Flere filer å forstå, og Kotlin DSL/IDE kan i noen tilfeller gi svakere editorstøtte enn faktisk Gradle-build
- **Vurdering:** Beste balanse mellom vedlikeholdbarhet og eksplisitthet

### Alternativ C: `buildSrc` eller precompiled script plugins
- **Fordeler:** Enda sterkere strukturering og mulighet for gjenbruk av buildlogikk
- **Ulemper:** Mer komplekst oppsett, mer overhead enn nødvendig for dagens behov
- **Vurdering:** Kan vurderes senere dersom buildlogikken vokser betydelig

## Nav-spesifikke vurderinger

### Plattform
- Gradle version catalog er godt støttet i moderne Gradle-oppsett
- Løsningen fungerer uten å endre deploy- eller runtime-konfigurasjon
- Endringen påvirker primært utvikleropplevelse og vedlikehold

### Team-påvirkning
- Nye avhengigheter bør registreres i `gradle/libs.versions.toml`
- Oppgraderinger blir enklere å reviewe fordi versjonsendringer blir mer isolert
- Testavhengigheter kan grupperes i bundles for mer idiomatisk Gradle-oppsett

## Konsekvenser

### Positive
- Én tydelig kilde for plugin- og biblioteksversjoner
- Renere `build.gradle.kts`
- Enklere å oppgradere biblioteker og forstå avhengighetsbildet
- Mindre duplisering i testoppsettet

### Negative
- Endringen introduserer en ekstra fil som utviklere må kjenne til
- `libs`-basert Kotlin DSL kan gi editorvarsler selv når Gradle-bygget fungerer

### Risiko
- Inkonsekvent bruk av versjonskatalog kan gjøre oppsettet forvirrende hvis nye avhengigheter legges direkte i `build.gradle.kts`
- Aliasnavn må holdes konsistente for å unngå dårlig lesbarhet

## Aksjonspunkter

- [x] Opprett `gradle/libs.versions.toml`
- [x] Flytt plugin- og biblioteksversjoner ut av `build.gradle.kts`
- [x] Innfør bundles for testavhengigheter
- [x] Verifiser at `./gradlew test` fortsatt fungerer
- [ ] Etabler teampraksis for navngivning av aliaser og bundles


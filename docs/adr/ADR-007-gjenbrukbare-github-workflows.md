# ADR-007: Gjenbrukbare GitHub Actions-workflows for build og deploy

**Dato:** 2026-06-16  
**Status:** Godkjent  
**Beslutningstakere:** Team Sikkerhetstjenesten

## Kontekst

`deploy.yml` og `deploy-branch.yml` hadde i stor grad duplisert logikk for:

- checkout
- Java- og Gradle-oppsett
- bygging og testing
- publisering av Docker-image
- attestering og signering
- deploy til NAIS

Dette gjorde workflowene mer krevende å vedlikeholde:

- samme endring måtte gjøres flere steder
- det var lett å få drift mellom branch- og main-workflowene
- review av pipeline-endringer ble mer støyende enn nødvendig

Samtidig hadde workflowene noen legitime forskjeller:

- ulike base-images
- forskjellig checkout-strategi for `push` og `pull_request`
- ulike målclustersett ved deploy

## Beslutning

Vi deler workflow-oppsettet i tre nivåer:

1. **Composite action** for delte build-steg  
   `/.github/actions/build-and-publish/action.yml`
2. **Reusable workflow** for build-jobben  
   `/.github/workflows/build-reusable.yml`
3. **Reusable workflow** for deploy-jobben  
   `/.github/workflows/deploy-reusable.yml`

Toppnivå-workflowene `deploy.yml` og `deploy-branch.yml` skal i hovedsak bare uttrykke:

- trigger
- miljøspesifikke inputs
- hvilke clusters som skal deployes til
- hvilken checkout-kontekst som skal brukes

### Input-basert variasjon

Forskjeller mellom workflowene håndteres gjennom inputs, blant annet:

- `base-image`
- `repository`
- `ref`
- `fetch-depth`
- `clusters`

Dette gjør at vi kan beholde nødvendig fleksibilitet uten å kopiere hele jobber.

## Alternativer vurdert

### Alternativ A: Beholde to separate workflowfiler
- **Fordeler:** Enkelt å lese hver workflow isolert
- **Ulemper:** Høy duplisering, økt vedlikeholdskostnad, større risiko for avvik
- **Vurdering:** For mye repetisjon for dagens behov

### Alternativ B: Kun bruke composite action (delvis valgt tidligere)
- **Fordeler:** Gjenbruk av steg uten å endre workflowstruktur for mye
- **Ulemper:** Jobbdefinisjoner og deploy-oppsett forblir duplisert
- **Vurdering:** Bra første steg, men utilstrekkelig alene

### Alternativ C: Reusable workflows + composite action (valgt)
- **Fordeler:** Tydelig separasjon mellom steg-gjenbruk og jobb-gjenbruk, mindre duplisering, enklere videreutvikling
- **Ulemper:** Mer indirection og flere filer å navigere i
- **Vurdering:** Beste balanse mellom fleksibilitet og vedlikeholdbarhet

## Nav-spesifikke vurderinger

### Sikkerhet og plattform
- GitHub-token brukes eksplisitt som secret inn i reusable build-workflowen
- Deploy-workflowen holder fast på NAIS-deploymekanismen og eksisterende cluster-konvensjoner
- Signering og attestering beholdes som del av standard build-flyt

### Team-påvirkning
- Pipeline-endringer kan nå gjøres ett sted og få effekt for både `main` og branch-deploy
- Workflowene blir enklere å lese for utviklere som primært trenger å forstå trigger og inputverdier
- Feilsøking krever noe mer kjennskap til reusable workflows og composite actions

## Konsekvenser

### Positive
- Betydelig mindre duplisering i CI/CD-konfigurasjonen
- Lavere risiko for at `deploy.yml` og `deploy-branch.yml` glir fra hverandre
- Endringer i build- eller deployflyt kan gjøres mer konsistent
- Toppnivå-workflowene blir kortere og tydeligere

### Negative
- Mer indirekte kontrollflyt enn i flate workflowfiler
- Flere GitHub Actions-filer å holde oversikt over
- Lokalt review av pipeline krever at man følger kallkjeden mellom workflows og actions

### Risiko
- Feil i en reusable workflow påvirker flere caller-workflows samtidig
- Endringer i inputs eller outputs må gjøres kompatibelt for alle brukere

## Aksjonspunkter

- [x] Opprett composite action for delt build/publish-logikk
- [x] Opprett reusable workflow for build-jobb
- [x] Opprett reusable workflow for deploy-jobb
- [x] Oppdater `deploy.yml` til å bruke de gjenbrukbare komponentene
- [x] Oppdater `deploy-branch.yml` til å bruke de gjenbrukbare komponentene
- [x] Verifiser YAML-syntaks for alle berørte workflowfiler
- [ ] Vurder å legge til workflow-tester eller linting spesifikt for GitHub Actions-konfigurasjon


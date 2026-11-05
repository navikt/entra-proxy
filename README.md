# Entra-proxy

Spring Boot-tjeneste som gir Nav-applikasjoner tilgang til ansattdata fra Entra ID, NORG og NOM.

Tjenesten henter ut:

1. Ansatts tematilganger
2. Ansatts enhetstilhørighet
3. Medlemmer i en bestemt enhet
4. Medlemmer i en bestemt arkivtema-gruppe
5. Utvidet informasjon om ansatt basert på navIdent eller T-ident
6. Ansattes grupper (bare SecEnabled)
7. Enhetsnavn fra NORG via `GET /api/v1/navn/{enhetsnummer}` (krever innlogging eller gyldig token, returnerer `text/plain`)

## Kom i gang

**Forutsetninger:** JDK 27 (se `.java-version`), Node.js 24 eller nyere og npm.

```bash
git clone https://github.com/navikt/entra-proxy
cd entra-proxy
./gradlew build      # kompilerer, kjører tester og bygger frontend
./gradlew bootRun    # starter appen lokalt
```

Kjør kodeanalyse med `./gradlew detekt`.

### Frontend

Skriptene til utforskersidene ligger i `src/main/typescript/`. HTML og CSS
ligger i `src/main/resources/static/`. Gradle kjører `npm ci` og kompilerer
skriptene ved `processResources`, slik at `bootRun` og `bootJar` inkluderer
JavaScript-modulene. Genererte filer ligger under `build/` og skal ikke sjekkes inn.

```bash
npm ci && npm run build        # kompiler separat
npm run playwright:install     # installer Chromium til frontend-testene
npm test                       # lint, bygg og Playwright-tester
./gradlew frontendTest         # samme tester via Gradle (kjøres også av ./gradlew check)
```

På Linux kan systemavhengigheter installeres med
`npm run playwright:install -- --with-deps`. Testene bruker en lokal statisk
server og simulerte API-svar, så Spring Boot trenger ikke å kjøre.
Feilspor lagres i `build/playwright-results/`.

## Tech stack

- **Språk:** Kotlin 2.4, JDK 27
- **Rammeverk:** Spring Boot 4.1
- **Cache og sesjoner:** Valkey
- **Frontend:** TypeScript, esbuild, Playwright
- **Test:** Kotest, Playwright
- **Plattform:** Nais (GCP)
- **Auth:** Entra ID (OBO, client credentials og Authorization Code Flow for utforskersidene)

## API

Produksjons-API-et ligger under `/api/v1`. Endepunkter for manuell testing ligger under `/dev`.
Se Swagger for full oversikt.

| Miljø | Swagger |
|-------|---------|
| Dev | <https://entraproxy.intern.dev.nav.no/swagger-ui/index.html#/> |
| Prod | <https://entraproxy.intern.nav.no/swagger-ui/index.html#/> |

Dev har to kontrollere: `dev` for manuell testing og `API/V1` for integrasjon mot systemer.

## Konfigurasjon

Settes i `.nais/naiserator.yaml` og i `.nais/dev-gcp.json` / `.nais/prod-gcp.json`.

| Variabel | Beskrivelse |
|----------|-------------|
| `GROUPS_DISABLED` | Deaktiverte grupper |
| `GROUPS_UTFORSKER` | Entra-gruppe som gir tilgang til utforskersidene |
| `NOMGRAPH` | URL til NOM GraphQL |
| `NAIS_CLUSTER_TYPE` | Klyngetype (dev/prod) |
| `NAIS_CLUSTER_CACHE` | Valkey-instans |

## Cache

Løsningen har flere cacher:

| Data | Varighet |
|------|----------|
| Kobling mellom _navident_ og _entraOid_ | 365 dager |
| Ansatts enhetstilhørighet | 3 timer |
| Ansatts tematilganger | 3 timer |
| Medlemmer i en enhet-gruppe | 3 timer |
| Medlemmer i en arkivtema-gruppe | 3 timer |

## Deploy

Deployes til Nais via GitHub Actions (`.github/workflows/`). Manifestene ligger i `.nais/`.

## Observability

- **Helse:** `/monitoring/health/liveness` og `/monitoring/health/readiness`
- **Metrikker:** `/monitoring/prometheus`
- **Alerts:** `.nais/alerts.yaml`, `.nais/probe-alerts.yaml`

## Dokumentasjon

- [Arkitekturbeslutninger (ADR)](docs/adr/README.md)
- [Confluence](https://confluence.adeo.no/spaces/TM/pages/758383588/entra-proxy)

## Team

Team Sikkerhetstjenesten. Spørsmål og support:
[#team-sikkerhetstjenesten](https://nav-it.slack.com/archives/C09KKNS0RJS)

## Lisens

Se [LICENSE](LICENSE).

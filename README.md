# Entra-proxy
Tjeneste for å hente ut: 
1) Ansatts tematilganger 
2) Ansatts enhetstilhørighet
3) Medlemmer i en bestemt enhet
4) Medlemmer i en bestemt arkivtema-gruppe
5) Hente utvidet informasjon om ansatt basert på navIdent eller T-ident
6) Hente Ansattes grupper (bare SecEnabled)
7) Hente enhetsnavn fra NORG via `GET /api/v1/navn/{enhetsnummer}` (krever innlogging eller gyldig token; returnerer `text/plain`)

## Frontend-utvikling

Skriptene til utforskersidene ligger i `src/main/typescript/`. HTML og CSS
ligger fortsatt i `src/main/resources/static/`, og sider uten skript er uendret.
Node.js 24 eller nyere og npm kreves ved bygging. Gradle installerer
avhengigheter med `npm ci` og kompilerer skriptet ved `processResources`,
slik at `bootRun` og `bootJar` inkluderer JavaScript-modulene. Genererte filer ligger
under `build/` og skal ikke sjekkes inn.

Kompiler separat med `npm ci && npm run build`. Installer nettleseren for
frontend-testene med `npm run playwright:install` etter `npm ci`.
På Linux kan systemavhengigheter installeres med
`npm run playwright:install -- --with-deps`.

Kjør testene med `npm test` eller `./gradlew frontendTest`. Testene bruker
Playwright og Chromium med en lokal statisk server og simulerte API-svar;
Spring Boot trenger ikke å kjøre. Testene kjøres også av `./gradlew check`.
Feilspor lagres i `build/playwright-results/`.

## Swagger og dokumentasjon

DEV: https://entraproxy.intern.dev.nav.no/swagger-ui/index.html#/
<br> Dev har to forskjellige kontroller: ```dev``` for manuell testing og ```API/V1```  for integrasjon mot systemer.
    
PROD: https://entraproxy.intern.nav.no/swagger-ui/index.html#/

Dokumentasjon:
https://confluence.adeo.no/spaces/TM/pages/758383588/entra-proxy

## Slackkanal for spørsmål og support:
[#team-sikkerhetstjenesten](https://nav-it.slack.com/archives/C09KKNS0RJS)


## Løsningen har flere cacher
* Kobling mellom _navident_ og _entraOid_ caches i 365 dager. 
* En ansatts enhetstilhørighet caches i 3 timer.
* En ansatts tematilganger caches i 3 timer
* Medlemmer i en enhet-gruppe caches i 3 timer.
* Medlemmer i en arkivtema-gruppe caches i 3 timer.

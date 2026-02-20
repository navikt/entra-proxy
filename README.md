# Entra-proxy
Tjeneste for å hente ut: 
1) Ansatts tematilganger 
2) Ansatts enhetstilhørighet
3) Medlemmer i en bestemt enhet
4) Medlemmer i en bestemt arkivtema-gruppe
5) Hente utvidet informasjon om ansatt basert på navIdent eller T-ident
6) Hente Ansattes grupper (bare SecEnabled)

Swagger:

DEV: https://entraproxy.intern.dev.nav.no/swagger-ui/index.html#/
    Dev har to forskjellige kontroller Dev for manuell testing og API/V1  for integrasjon mot systemer.
    
PROD: https://entraproxy.intern.nav.no/swagger-ui/index.html#/

Dokumentasjon:
https://confluence.adeo.no/spaces/TM/pages/758383588/entra-proxy

Slackkanal for spørsmål og support:
#team-sikkerhetstjenesten

## CodeQL Code Scanning

This repository uses Java 25, which is not supported by the default CodeQL autobuild.
A custom CodeQL workflow (`.github/workflows/codeql.yml`) is configured with manual build mode to handle this.

**If you see failing "Code Quality: CodeQL Setup" runs**, the default CodeQL setup needs to be disabled:

1. Go to **Settings → Code security and analysis**
2. Under **Code scanning → CodeQL analysis**, click **Switch to advanced**
3. This disables the default setup and lets the custom workflow in `.github/workflows/codeql.yml` handle analysis


Løsningen har flere cacher
* Kobling mellom _navident_ og _entraOid_ caches i 365 dager. 
* En ansatts enhetstilhørighet caches i 3 timer.
* En ansatts tematilganger caches i 3 timer
* Medlemmer i en enhet-gruppe caches i 3 timer.
* Medlemmer i en arkivtema-gruppe caches i 3 timer.

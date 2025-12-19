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


Løsningen har flere cacher
* Kobling mellom _navident_ og _entraOid_ caches i 365 dager. 
* En ansatts enhetstilhørighet caches i 3 timer.
* En ansatts tematilganger caches i 3 timer
* Medlemmer i en enhet-gruppe caches i 3 timer.
* Medlemmer i en arkivtema-gruppe caches i 3 timer.

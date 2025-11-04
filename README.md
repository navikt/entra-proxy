# Entra-proxy
Tjeneste for å hente ut: 
1) Ansatts tematilganger 
2) Ansatts enhetstilhørighet
3) Medlemmer i en bestemt enhet
4) Medlemmer i en bestemt arkivtema-gruppe


Swagger-dokumentasjon:

DEV: https://entra-proxy.dev.intern.nav.no/swagger/index.html 
    Devkontroller er tilgjengelig kun i dev-gcp og er ment kun for enkel test/visuell sjekk, mens EntraController er den som skal brukes for system i produksjon.

PROD: https://entra-proxy.intern.nav.no/swagger/index.html

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

# ADR-003: Separasjon mellom produksjons-API og dev-endepunkter

**Dato:** 2026-06-16  
**Status:** Godkjent  
**Beslutningstakere:** Team Sikkerhetstjenesten

## Kontekst

`entra-proxy` har to ulike behov som lett kan komme i konflikt:

1. Et produksjonsklart API for interne konsumenter med tydelige sikkerhetskrav
2. En enkel måte å teste oppslag og feilsøke integrasjoner i dev-miljøer

Disse behovene er tydelige i dagens kode:

- `EntraController` eksponerer produksjonsendepunkter under `/api/v1`
- `DevEntraController` eksponerer egne dev-endepunkter under `/dev`
- produksjonsendepunktene er beskyttet med `ProtectedRestController` og Azure AD-validering
- dev-endepunktene er mer åpne, men bare tilgjengelige utenfor prod via `@ConditionalOnNotProd`

Produksjons-API-et håndterer samtidig både OBO- og CC-scenarier, der tokenets innhold avgjør hvilke operasjoner som er tillatt.

## Beslutning

Vi holder **produksjons-API** og **dev-/testendepunkter** eksplisitt adskilt i egne kontrollere, egne URL-rom og egne tilgjengelighetsregler.

Dette betyr:

- produksjons-API-et eksponeres under `/api/v1`
- dev-endepunkter eksponeres separat og er ikke tilgjengelige i prod
- tokenkrav og autorisasjonslogikk håndheves i produksjonskontrolleren
- dev-kontrolleren kan prioritere enkel testbarhet og feilsøking uten å påvirke det offentlige API-et

## Hvorfor dette er valgt

Denne separasjonen gjør det mulig å optimalisere for to ulike hensyn uten å blande dem:

- stabilitet og sikkerhet i produksjon
- rask utvikling, manuell testing og diagnostikk i dev

Den reduserer også risikoen for at testendepunkter eller svakere beskyttede flyter utilsiktet blir del av produksjonskontrakten.

## Alternativer vurdert

### Alternativ A: Ett felles API med profilerte if-setninger eller feature-flagg
- **Fordeler:** Færre kontrollere og færre URL-er
- **Ulemper:** Mer kompleks controllerlogikk og større risiko for at dev-funksjonalitet eksponeres feil
- **Vurdering:** Mindre tydelig og mer risikofylt

### Alternativ B: Egne kontrollere og egne URL-rom for prod og dev (valgt)
- **Fordeler:** Tydelige grenser, enklere sikkerhetsforståelse og enklere dokumentasjon av hva som er produksjonskontrakt
- **Ulemper:** Noe duplisering i endepunkter og controllerkode
- **Vurdering:** Klarest og tryggest for denne typen intern tjeneste

### Alternativ C: Egen separat dev-applikasjon
- **Fordeler:** Full isolasjon mellom prod og testfunksjoner
- **Ulemper:** Mer drift, mer konfigurasjon og høyere vedlikeholdskostnad
- **Vurdering:** For tungt for dagens behov

## Arkitektoniske føringer

### Produksjons-API
- Produksjonsendepunkter skal være stabile og eksplisitt dokumenterte
- Tokenbaserte regler for OBO og CC skal håndheves i eller tett på controllerlaget
- Produksjonskontrakten skal ikke avhenge av dev-spesifikk adferd

### Dev-endepunkter
- Dev-endepunkter skal være eksplisitt avgrenset til ikke-produksjonsmiljøer
- Dev-endepunkter kan prioritere testbarhet og manuell inspeksjon
- Endepunkter som kun er nyttige for feilsøking, skal ikke automatisk bli del av produksjons-API-et

### Vedlikehold
- Felles domene- og tjenestelogikk skal fortsatt ligge i tjenestelaget
- Eventuell duplisering i controllerlaget aksepteres når den gir tydeligere miljøgrenser

## Konsekvenser

### Positive
- Tydeligere skille mellom støttet produksjonskontrakt og interne testflater
- Lavere risiko for feil eksponering av dev-funksjonalitet i prod
- Enklere å resonere om sikkerhetsnivå per endepunkt
- Bedre støtte for manuell testing i dev uten å kompromisse produksjonsdesignet

### Negative
- Noe duplisering i controllerkode og endepunktsdefinisjoner
- Dokumentasjon må passe på å skille tydelig mellom dev- og prod-flater

### Risiko
- Funksjonalitet kan utilsiktet ende opp bare i dev-controlleren dersom endringer ikke gjennomgås bevisst
- For mye duplisering i controllerlaget kan gjøre API-ene vanskeligere å holde synkrone

## Aksjonspunkter

- [x] Behold separat `EntraController` for produksjon og `DevEntraController` for dev
- [x] Behold `@ConditionalOnNotProd` for dev-controlleren
- [x] Behold eksplisitt tokenkontroll for OBO og CC i produksjons-API-et
- [ ] Vurder å dokumentere hvilke endepunkter som er del av støttet produksjonskontrakt versus rene dev-verktøy
- [ ] Vurder å redusere controllerduplisering med interne hjelpetjenester dersom lesbarheten svekkes over tid



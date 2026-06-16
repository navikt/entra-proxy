# ADR-002: Cache som del av lesearkitekturen

**Dato:** 2026-06-16  
**Status:** Godkjent  
**Beslutningstakere:** Team Sikkerhetstjenesten

## Kontekst

`entra-proxy` gjør mange lesekall mot eksterne tjenester for samme type informasjon:

- oppslag fra `navIdent` til Entra OID
- henting av temaer og enheter for ansatt
- oppslag av gruppemedlemmer
- oppslag av utvidet ansattinformasjon
- oppslag av enhetsnavn i NORG

Disse kallene er relativt kostbare sammenlignet med lokal prosessering:

- de går over nettverk
- de avhenger av tilgjengelighet og responstid i eksterne systemer
- de kan repeteres ofte for samme nøkkel
- flere kall bygger på hverandre, for eksempel OID-oppslag før videre Entra-kall

Løsningen er allerede bygd med Spring Cache, Redis/Valkey og flere cachekonfigurasjoner med ulik TTL:

- `entraoid`: 365 dager
- `graph`: 3 timer
- `medlemmer`: 3 timer
- `norg`: 3 timer

I tillegg finnes det mekanismer for cacheoppfrisking og for å rydde OID-cache ved `NotFound` fra Entra.

## Beslutning

Caching er en **førsteklasses del av lesearkitekturen**, ikke bare en opportunistisk optimalisering.

Vi cacher resultatene av sentrale lesekall i tjenestelaget, med TTL tilpasset datatypen:

- identitetskobling (`navIdent` → OID) caches lenge
- tilgangs- og medlemskapsdata caches moderat
- berikelsesdata fra NORG caches moderat

Cache brukes sammen med kontrollert oppfrisking og fallback ved foreldede OID-er.

## Hvorfor dette er valgt

Løsningen trenger raske og stabile oppslag for interne brukere, samtidig som kildesystemene ikke bør belastes unødvendig.

Cache-strategien er valgt fordi den:

- reduserer antall kall til Entra og NORG
- reduserer svartid for vanlige oppslag
- reduserer sårbarheten for kortvarig treghet i nedstrømsystemer
- gjør det mulig å håndtere kjente datamønstre med forskjellig levetid

Den lange TTL-en for OID-oppslag gjenspeiler at koblingen vanligvis er stabil, mens tilgangsdata får kortere TTL fordi gruppemedlemskap og organisasjonstilknytning kan endre seg oftere.

## Alternativer vurdert

### Alternativ A: Ingen caching
- **Fordeler:** Alltid ferske data, mindre intern tilstand
- **Ulemper:** Høy belastning på eksterne systemer, høyere latenstid, større sårbarhet ved ustabilitet
- **Vurdering:** Ikke egnet for denne typen lesetjeneste

### Alternativ B: Cache i tjenestelaget med differensierte TTL-er (valgt)
- **Fordeler:** God ytelse, mindre last på kildesystemer, tydelig plassering av cache-ansvar
- **Ulemper:** Risiko for delvis foreldede data og behov for tydelig forståelse av cachebruken hos utviklere
- **Vurdering:** Beste balanse mellom konsistens, ytelse og enkelhet

### Alternativ C: Kun HTTP-/gateway-cache utenfor applikasjonen
- **Fordeler:** Mindre applikasjonslogikk
- **Ulemper:** Vanskeligere nøkkelstyring, svakere domeneinnsikt og vanskeligere oppfrisking per metode og nøkkel
- **Vurdering:** Ikke presist nok for dagens bruksmønstre

## Arkitektoniske føringer

### Plassering av cache
- Caching skal i hovedsak ligge på tjenestemetoder, ikke i kontrollerne
- Cache-nøkler skal være stabile og domeneorienterte
- Ulike datatyper skal ha egne cacher når de har ulik levetid eller oppfriskingsbehov

### Konsistensstrategi
- Vi aksepterer eventual consistency for lesedata
- Ved `NotFound` på kall som er avhengige av cached OID, skal OID kunne hentes på nytt etter cache-rydding
- Planlagt eller eksplisitt oppfrisking kan brukes der det er nødvendig for å redusere effekten av foreldede nøkler

### Operasjonelle hensyn
- Redis/Valkey er en del av den operative arkitekturen i GCP-miljøene
- Cache-tilstand må kunne observeres via metrics og health-endepunkter der det er hensiktsmessig
- Feil i cachelaget må ikke føre til uklar funksjonell oppførsel

## Konsekvenser

### Positive
- Raskere responstid for gjentatte oppslag
- Lavere belastning på Entra og NORG
- Bedre robusthet mot midlertidig treghet i eksterne systemer
- Klarere skille mellom identitetsdata, tilgangsdata og berikelsesdata

### Negative
- Data kan være midlertidig foreldede
- Cachefeil og nøkkelkollisjoner kan være vanskelige å feilsøke
- Utviklere må forstå TTL og oppfriskingsregler for å endre løsningen trygt

### Risiko
- For lange TTL-er kan gi for gamle tilgangsdata
- For korte TTL-er kan undergrave ytelsesgevinstene
- Inkonsekvent nøkkelstrategi kan gjøre oppfrisking og sletting upålitelig

## Aksjonspunkter

- [x] Behold separate cacher for OID, Graph-data, medlemmer og NORG-data
- [x] Behold forskjellig TTL for ulike datatyper
- [x] Behold fallback som rydder OID-cache ved `NotFound`
- [ ] Vurder å dokumentere cache-navn, TTL og nøkkelstrategi samlet i teknisk dokumentasjon
- [ ] Vurder egne metrics eller dashboards for cache hit/miss og oppfriskingsfeil



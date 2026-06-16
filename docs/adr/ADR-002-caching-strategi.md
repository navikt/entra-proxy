# ADR-002: Valkey (Redis) for distribuert cache med resilient feilhåndtering

**Dato:** 2025-03-01  
**Status:** Godkjent  
**Beslutningstakere:** Team Tilgangsmaskin

## Kontekst

Applikasjonen gjør oppslag mot eksterne tjenester (PDL, NOM, skjerming, vergemål) som er kostbare og trege. Med flere replika-pods og høy trafikk (bulk-oppslag med opptil 1000 brukere) trenger vi en distribuert cache som deles mellom pods.

Uten cache:
- Hvert API-kall trigger kjedede oppslag mot PDL GraphQL + PIP-tjenester
- Latens øker fra ~50ms til 500ms+ per bruker
- Eksterne tjenester får unødvendig last

## Beslutning

Vi bruker **Valkey (Redis-kompatibel)** via Nais med følgende resiliens-strategi:

1. **Valkey-instans** provisjonert via `naiserator.yaml` (`spec.valkey`)
2. **Spring Cache abstraction** med `RedisCacheManager`
3. **Per-cache TTL** konfigurert per `CachableRestConfig`
4. **ResilientRedisSerializer** — fanger `SerializationException` ved deserialisering, returnerer `null` (cache miss) i stedet for å kaste feil
5. **CacheMeteredErrorHandler** — logger og teller alle cache-operasjonsfeil (GET/PUT/EVICT) uten å la dem propagere til forretningslogikk
6. **Caffeine** som fallback for lokal caching av statiske data

Effekten: Cache-feil (timeout, serialisering, nettverksfeil) degraderer aldri tjenesten — applikasjonen faller tilbake til direkte oppslag.

## Alternativer vurdert

### Alternativ A: Valkey med resilient error handling (valgt)
- **Fordeler:** Distribuert, delt mellom pods, Nais-native, graceful degradation
- **Ulemper:** Ekstra infrastruktur, serialiseringskompleksitet
- **Nav-vurdering:** Nais støtter Valkey native — minimal operasjonell overhead

### Alternativ B: Kun lokal cache (Caffeine)
- **Fordeler:** Enklere, ingen nettverksavhengighet, raskere
- **Ulemper:** Hver pod har sin egen cache, duplikerte oppslag, inkonsistens mellom pods
- **Nav-vurdering:** Fungerer for statiske oppslag, men ikke for person-data som endres

### Alternativ C: Ingen cache
- **Fordeler:** Enklest, alltid ferske data
- **Ulemper:** Uakseptabel latens ved bulk, høy last mot PDL
- **Nav-vurdering:** Bryter SLA-krav og belaster delte tjenester unødvendig

## Nav-spesifikke vurderinger

### Sikkerhet
- Dataklassifisering: **Fortrolig** — personopplysninger i cache
- Cache-innhold: Brukerdata fra PDL (familierelasjon, adressebeskyttelse, geografisk tilknytning)
- Nettverksisolering: Valkey-instansen er kun tilgjengelig innenfor namespace
- TTL: Sikrer at data ikke lagres lenger enn nødvendig

### Plattform
- Nais-konfigurasjon: `spec.valkey[].instance` + `access: readwrite`
- Ressursbehov: Valkey-instans provisjoneres av Nais
- Observerbarhet: `cache.operation.failed`-counter, `cache.deserialize.failed`-counter, Spring Cache statistics

### Team-påvirkning
- Berørte team: Kun Team Tilgangsmaskin
- Migrasjonsstrategi: N/A (eksisterende løsning)
- Tilbakerulle-strategi: Fjern cache → applikasjonen fungerer, bare tregere

## Konsekvenser

### Positive
- Cache-feil gir aldri 500 til konsumenter
- Metrikker gir innsikt i cache-helseproblemer før de eskalerer
- Enkel å slå av (cache evict/flush) uten redeploy

### Negative
- Serialiseringsformat (Jackson type info) kan brekke ved modellendringer → cache miss (akseptabelt)
- Ekstra kompleksitet i serializer-wrapping

### Risiko
- Valkey-nedetid gir økt latens (ikke feil) — mitigert med timeout-config og health indicator

## Aksjonspunkter

- [x] Implementer `ResilientRedisSerializer`
- [x] Implementer `CacheMeteredErrorHandler`
- [x] Konfigurer per-cache TTL via `CachableRestConfig`
- [x] Sett opp Grafana-dashboard for cache-metrikker
- [ ] Vurder cache-warming ved oppstart for hyppig brukte oppslag


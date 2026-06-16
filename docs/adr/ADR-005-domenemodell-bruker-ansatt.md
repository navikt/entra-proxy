# ADR-005: Separasjon av Bruker- og Ansatt-domene med ekstern databerikelse

**Dato:** 2025-01-15  
**Status:** Godkjent  
**Beslutningstakere:** Team Tilgangsmaskin

## Kontekst

Tilgangskontrollen evaluerer regler mellom to parter: **ansatt** (Nav-medarbeider) og **bruker** (person i befolkningen). Begge må berikes med data fra eksterne tjenester for at regler kan evalueres:

- **Bruker**: PDL (folkeregister), vergemål, skjerming, geografisk tilknytning
- **Ansatt**: Azure AD (grupper), NOM (organisasjonsdata), Oppfølging (enhetstilknytning)

Utfordring:
- Data kommer fra mange kilder med ulike oppdateringsintervaller
- Noen data er hendelsesdrevet (Kafka), andre er synkrone (REST/GraphQL)
- Modellen må være enkel nok for regelmotor å evaluere

## Beslutning

Vi separerer domenet i to aggregate roots med tydelig databerikelsesmønster:

### Bruker-domenet
- `Bruker` — aggregat med: `BrukerId`, `Familie` (Set<BrukerId>), `GeografiskTilknytning`, adressebeskyttelse, dødsfall, vergemål, skjerming
- **Datakilde:** PDL GraphQL (synkront ved oppslag) + PDL Kafka-topic (asynkron oppdatering)
- **Cache:** Valkey med TTL per oppslagstype

### Ansatt-domenet
- `Ansatt` — aggregat med: `AnsattId`, AD-grupper, NOM-data, oppfølgingsenhet
- **Datakilde:** Entra Proxy (AD-grupper, synkront) + NOM Kafka (asynkron) + Oppfølging Kafka (asynkron)
- **Cache:** Valkey for REST-oppslag, JPA for Kafka-baserte data

### Berikelsesmønster
```
API-kall → AnsattTjeneste.ansatt(id) → berik fra cache/DB/REST
         → BrukerTjeneste.bruker(id)  → berik fra cache/REST
         → RegelMotor.evaluer(ansatt, bruker)
```

### Familiemodell
`Familie` er forenklet til `Set<BrukerId>` for fire kategorier: foreldre, barn, søsken, partnere. Ingen relasjonstype-enum — hvilken kategori et familiemedlem tilhører defineres av settet det ligger i.

## Alternativer vurdert

### Alternativ A: Separerte aggregater med ekstern berikelse (valgt)
- **Fordeler:** Klar ansvarsfordeling, regler får ferdig-berikede objekter, enkel testing
- **Ulemper:** Mange kilder å koordinere, latens ved berikelse
- **Nav-vurdering:** Følger domain-driven design, team-autonomi ivaretatt

### Alternativ B: Én samlet "TilgangskontrollContext" med lazy loading
- **Fordeler:** Kun henter data som trengs per regel
- **Ulemper:** Implisitt avhengigheter, vanskelig å teste, N+1-problemer
- **Nav-vurdering:** Bryter med eksplisitt design-prinsipp

### Alternativ C: Hendelsesdrevet materialisert view
- **Fordeler:** All data lokalt, raskest mulig evaluering
- **Ulemper:** Konsistensvindu, enormt datamengde, kompleks synkronisering
- **Nav-vurdering:** Interessant for fremtiden, men for komplekst nå

## Nav-spesifikke vurderinger

### Sikkerhet
- Dataklassifisering: **Strengt fortrolig** — PII fra PDL
- Auth-mekanisme: Maskin-til-maskin (Azure AD CCF) mot PDL, NOM, skjerming
- PII-håndtering: Fødselsnummer maskeres i logger, cache-data er fortrolig

### Plattform
- Nais-konfigurasjon: Outbound access policy til PDL, NOM, Entra Proxy, skjerming
- Ressursbehov: Primært nettverks-I/O (GraphQL-kall)
- Observerbarhet: Timed oppslag, cache-hit/miss metrikker, health indicators per ekstern tjeneste

### Team-påvirkning
- Berørte team: PDL-teamet (dataeier), NOM-teamet (dataeier)
- Migrasjonsstrategi: N/A
- Tilbakerulle-strategi: N/A (grunnleggende arkitektur)

## Konsekvenser

### Positive
- Regler er rene funksjoner: `(Ansatt, Bruker) → Boolean` — enkel å teste
- Datahenting og regellogikk er decoupled
- Caching kan optimeres per datakilde uavhengig

### Negative
- Latens ved cold cache (mange eksterne kall)
- Konsistensvindu mellom Kafka-oppdatering og cache-evict

### Risiko
- PDL-endringer i API/schema krever oppdatering av mapper — mitigert med GraphQL typing
- NOM/Oppfølging Kafka-topics kan endre format — mitigert med `spring.json.value.default.type`-test

## Aksjonspunkter

- [x] Implementer `BrukerTjeneste` med PDL-berikelse
- [x] Implementer `AnsattTjeneste` med AD + NOM + Oppfølging
- [x] Forenkle `Familie` til `Set<BrukerId>` (fjern `FamilieMedlem`)
- [x] Kafka-konsumenter for NOM og Oppfølging med idempotent upsert
- [x] Verifiser default-type-konfigurering med refleksjonstest
- [ ] Vurder contract-tester mot PDL/NOM-skjemaer


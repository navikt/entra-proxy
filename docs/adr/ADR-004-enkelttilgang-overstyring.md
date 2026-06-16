# ADR-004: Tidsbegrenset overstyring av regelbrudd via enkelttilgang

**Dato:** 2025-01-15  
**Status:** Godkjent  
**Beslutningstakere:** Team Tilgangsmaskin

## Kontekst

Tilgangskontrollen kan avvise en ansatt fra å se en bruker på grunn av overstyrbare regler (f.eks. familierelasjon, geografisk tilhørighet). I visse situasjoner har den ansatte **legitime behov** for tilgang, for eksempel ved akutte saker. Nav-ansatte trenger en mekanisme for å overstyre avvisning med begrunnelse og tidsbegrensning.

Krav fra Datatilsynet og Navs retningslinjer:
- Overstyring skal logges (audit trail)
- Begrunnelse er påkrevd
- Tilgangen er tidsbegrenset
- Kun overstyrbare regler kan overstyres (ikke kode 6/7)

## Beslutning

Vi implementerer **enkelttilgang** som en tidsbegrenset overstyring lagret i databasen:

1. **Endepunkt:** `POST /api/v1/overstyr` — ansatt registrerer enkelttilgang med begrunnelse
2. **Lagring:** `EnkeltTilgangEntity` i PostgreSQL med JPA
3. **Validering:** `@EnkeltTilgangGyldig` (custom constraint) + `EnkeltTilgangValidator`
4. **Gyldighet:** Konfigurerbar varighet (`EnkeltTilgangConfig`)
5. **Regelmotor-integrasjon:** Ved `RegelException` sjekkes om ansatt har aktiv enkelttilgang for brukeren — isåfall gis tilgang likevel
6. **Bulk-støtte:** `tilganger(ansattId, brukerIds)` for effektiv bulk-sjekk
7. **Audit:** JPA `@EntityListeners` med audit-felter (opprettet av, tidspunkt)

Flyten:
```
Ansatt → kompletteRegler() → RegelException? 
    → harEnkeltTilgang(ansatt, bruker)? 
        → ja: tillat tilgang (logg)
        → nei: kast RegelException (avvis)
```

## Alternativer vurdert

### Alternativ A: Database-basert enkelttilgang (valgt)
- **Fordeler:** Fullstendig audit trail, tidsbegrenset, querybar, bulk-oppslag
- **Ulemper:** Database-avhengighet for tilgangssjekk, ekstra latens
- **Nav-vurdering:** Nødvendig for compliance — Datatilsynet krever sporbarhet

### Alternativ B: Token-basert overstyring (claim i JWT)
- **Fordeler:** Stateless, ingen DB-avhengighet
- **Ulemper:** Ingen sentral oversikt, vanskelig å trekke tilbake, tokenforfalskning
- **Nav-vurdering:** Bryter med Navs krav om sentralisert audit

### Alternativ C: Ingen overstyring (alltid følg regler)
- **Fordeler:** Enklest, ingen sikkerhetsrisiko ved misbruk
- **Ulemper:** Blokkerer saksbehandling i akutte situasjoner
- **Nav-vurdering:** Ikke akseptabelt — bryter med driftsbehov

## Nav-spesifikke vurderinger

### Sikkerhet
- Dataklassifisering: **Strengt fortrolig** — hvem som ser hvem er sensitiv info
- Auth-mekanisme: Kun OBO-token (ansatt autentisert) — ikke CCF
- PII-håndtering: BrukerId lagres, begrunnelse lagres, alt auditlogges
- Misbruksbeskyttelse: Begrunnelse påkrevd, tidsbegrensning, `EnkeltTilgangKonsumentValidator` validerer konsument

### Plattform
- Nais-konfigurasjon: Krever database (Cloud SQL) — allerede provisjonert
- Ressursbehov: Minimalt — enkle DB-oppslag
- Observerbarhet: JPA auditing, `@EntityListeners`

### Team-påvirkning
- Berørte team: Konsumerende team (de som kaller overstyr-endepunktet)
- Migrasjonsstrategi: N/A
- Tilbakerulle-strategi: Fjern endepunkt → eksisterende enkelttilganger utløper automatisk

## Konsekvenser

### Positive
- Nav-ansatte kan håndtere akutte saker uten å vente på regelendring
- Full sporbarhet (hvem, når, hvorfor, for hvem)
- Automatisk utløp — ingen manuell opprydding

### Negative
- Kompleksitet i regelmotor (sjekk etter exception)
- Potensielt misbruk (mitigert med validering og logging)
- Ekstra DB-kall per avvist tilgang

### Risiko
- Ansatte kan misbruke til å se opplysninger de ikke trenger — mitigert med:
  - Begrunnelseskrav
  - Tidsbegrensning
  - Audit-logging for kontroll i etterkant
  - Kun overstyrbare regler (kode 6/7 kan ALDRI overstyres)

## Aksjonspunkter

- [x] Implementer `EnkeltTilgangTjeneste` med database-lagring
- [x] Implementer `EnkeltTilgangGyldig` custom validator
- [x] Integrer med `RegelTjeneste.kompletteRegler()`
- [x] Integrer med bulk-flow
- [x] Legg til JPA audit-felter
- [ ] Vurder forenkling av enkelttilgang-flow (se kvalitetsanalyse)
- [ ] Sett opp alert ved unormalt mange overstyringer


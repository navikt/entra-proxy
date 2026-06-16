# ADR-001: Regelmotor med interface-baserte regler og Spring-komponentskanning

**Dato:** 2025-01-15  
**Status:** Godkjent  
**Beslutningstakere:** Team Tilgangsmaskin

## Kontekst

Populasjonstilgangskontroll trenger en motor som evaluerer tilgangsregler for om en ansatt i Nav kan se opplysninger om en bruker. Reglene er domene-definerte (f.eks. kode 6/7, egen ansatt, familierelasjon) og må kunne utvides uten å endre motorlogikken.

Kravene er:
- Nye regler skal kunne legges til uten endring i eksisterende kode (Open/Closed)
- Regler kjøres i definert rekkefølge (kortest evaluering først)
- To regelsett: **kjerne** (grunnleggende) og **komplett** (alle regler inkl. overstyrbare)
- Regler må støtte bulk-evaluering for ytelse
- Observerbarhet: hvilken regel avviste, tidsbruk per regel

## Beslutning

Vi bruker et **interface-basert regelmønster** med Spring-komponentskanning:

1. `Regel`-interface med `evaluer(ansatt, bruker): Boolean` og `metadata`
2. Sub-interfaces: `KjerneRegel`, `OverstyrbarRegel`, `TellendeRegel`
3. `@SortertRegel(n)` — custom annotation som kombinerer `@Component` og `@Order`
4. `RegelSett` — wraps en liste regler, injisert via qualifier (`KJERNE`/`KOMPLETT`)
5. `RegelMotor` — orkestrerer evaluering, kaster `RegelException` ved avvisning

## Alternativer vurdert

### Alternativ A: Interface + Spring (valgt)
- **Fordeler:** Automatisk oppdagelse av nye regler, deklarativ rekkefølge, enkel testing per regel
- **Ulemper:** Implisitt konfigurasjon via classpath scanning, rekkefølge kun synlig via annotation
- **Nav-vurdering:** Passer godt med Spring Boot-økosystemet teamet bruker

### Alternativ B: Eksplisitt regel-liste i konfig
- **Fordeler:** Eksplisitt, synlig rekkefølge i én fil
- **Ulemper:** Krever manuell oppdatering ved nye regler, mer boilerplate
- **Nav-vurdering:** Tryggere for små team, men skalerer dårlig med mange regler

### Alternativ C: DSL / regelspråk (Drools, Easy Rules)
- **Fordeler:** Ikke-utviklere kan endre regler, ekstern konfigurasjon
- **Ulemper:** Ekstra runtime-avhengighet, debugging vanskelig, sikkerhet ved dynamisk evaluering
- **Nav-vurdering:** Overkill — regelendringer styres av teamet og krever deploy uansett (audit trail)

## Nav-spesifikke vurderinger

### Sikkerhet
- Dataklassifisering: **Strengt fortrolig** (kode 6/7-opplysninger)
- Auth-mekanisme: Azure AD (OBO/CCF token)
- PII-håndtering: Fødselsnummer maskeres i logging

### Plattform
- Nais-konfigurasjon: Standard Spring Boot-app, ingen ekstra infra for motorlogikk
- Ressursbehov: CPU-bundet (ingen I/O i selve regelevalueringen)
- Observerbarhet: Micrometer-timer per regelsett, OTel span per evaluering

### Team-påvirkning
- Berørte team: Kun Team Tilgangsmaskin (intern arkitektur)
- Migrasjonsstrategi: N/A — greenfield
- Tilbakerulle-strategi: Standard deploy-rollback

## Konsekvenser

### Positive
- Ny regel = ny `@SortertRegel`-klasse — ingen endring i motor
- Hver regel er uavhengig testbar med unit-tester
- Bulk-evaluering gjenbruker samme regler

### Negative
- Rekkefølge er distribuert på tvers av klasser (ikke én oversikt)
- Feil i `@Order`-verdi kan gi uønsket evalueringsrekkefølge

### Risiko
- Mange regler kan gi ytelsesproblemer i bulk (mitigert med timing-metrikker)

## Aksjonspunkter

- [x] Implementer `Regel`-interface med sub-interfaces
- [x] Implementer `RegelMotor` med `RegelSett`-injeksjon
- [x] Legg til `@Timed` og `@WithSpan` for observerbarhet
- [ ] Vurder metrikk per enkelt-regel (ikke bare per regelsett)
- [ ] Dokumenter rekkefølge i README eller startupinfo


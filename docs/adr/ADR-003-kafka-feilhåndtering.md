# ADR-003: Eksponentiell backoff uten DLT for Kafka-konsumenter

**Dato:** 2025-06-01  
**Status:** Godkjent  
**Beslutningstakere:** Team Tilgangsmaskin

## Kontekst

Applikasjonen konsumerer hendelser fra Kafka-topics:
- **PDL Avro-topic** — livshendelsedata (fødsel, død, adressebeskyttelse, vergemål, etc.)
- **NOM** — organisasjonsdata (ansattopplysninger fra NOM)
- **Oppfølging** — oppfølgingsenhet per bruker

Feilhåndtering trengs fordi:
- Eksterne tjenester (PDL, DB) kan være midlertidig utilgjengelige
- Deserialiseringsfeil kan oppstå ved skjemaendringer
- Uten retry mistes hendelser permanent

## Beslutning

Vi bruker **én delt `CommonErrorHandler`** med eksponentiell backoff, **uten Dead Letter Topic (DLT)**:

1. **`DefaultErrorHandler`** med `ExponentialBackOff`:
   - Initial interval: 1 sekund
   - Multiplier: 2x
   - Max interval: 30 sekunder
   - Max elapsed time: 60 sekunder (deretter droppes meldingen)
2. **`DroppedMessageMeter`** (RetryListener):
   - `failedDelivery()`: Logger hvert retry-forsøk (WARN)
   - `recovered()`: Logger permanent tap av melding (ERROR) + inkrementerer `kafka.message.dropped`-counter
3. **Ingen per-listener error handlers** — alle konsumenter bruker den delte `CommonErrorHandler`
4. **Exceptions propageres** ut av listener-metoder (ingen `runCatching` i konsumenter)
5. Konsument-operasjoner (upsert) er **idempotente** — retry er trygt

## Alternativer vurdert

### Alternativ A: Eksponentiell backoff uten DLT (valgt)
- **Fordeler:** Enkelt, én felles konfigurering, metrikk-drevet overvåking av tap
- **Ulemper:** Meldinger tapt etter max elapsed time (60s), ingen manuell reprocessing
- **Nav-vurdering:** Akseptabelt — dataene fylles inn ved neste livshendelsemelding eller ved cache-evict

### Alternativ B: DLT (Dead Letter Topic)
- **Fordeler:** Ingen tap — feilede meldinger kan reprocesses manuelt
- **Ulemper:** DLT i annet namespace ikke støttet, operasjonell overhead (hvem overvåker DLT?), meldinger kan bli utdaterte
- **Nav-vurdering:** Overkill for cache-lignende data som oppdateres kontinuerlig

### Alternativ C: Uendelig retry
- **Fordeler:** Ingen melding tapt
- **Ulemper:** Blokkerer partisjon, konsument-lag øker, downstream-effekter
- **Nav-vurdering:** Bryter med Kafka-kontrakt og påvirker andre konsumenter

### Alternativ D: Per-listener error handler
- **Fordeler:** Granulær kontroll per topic
- **Ulemper:** Duplisert konfigurering, `KafkaListenerErrorHandler` svelger exception *før* `CommonErrorHandler` ser den → ingen retry
- **Nav-vurdering:** Anti-pattern — fjernet til fordel for delt handler

## Nav-spesifikke vurderinger

### Sikkerhet
- Dataklassifisering: **Fortrolig** (PDL-hendelser inneholder personnummer)
- Meldingsinnhold logges aldri (kun metadata: topic, partition, offset)

### Plattform
- Nais-konfigurasjon: `spec.kafka.pool: nav-{cluster}` — standard Kafka-pool
- Ressursbehov: Retry blokkerer thread i opptil 60s — akseptabelt med få partisjoner
- Observerbarhet: `kafka.message.dropped` counter + alerting ved terskelverdi

### Team-påvirkning
- Berørte team: Kun Team Tilgangsmaskin
- Migrasjonsstrategi: Eksisterende per-listener handlers fjernet, `runCatching` i NOM fjernet
- Tilbakerulle-strategi: Gjeninnfør per-listener handler (ikke anbefalt)

## Konsekvenser

### Positive
- Én konfigurering for alle konsumenter — enklere vedlikehold
- Midlertidige feil (DB-timeout, nettverk) håndteres automatisk
- Metrikker gir varsel ved permanent tap

### Negative
- Meldinger droppes etter 60 sekunder — akseptabelt tap for cache-data
- Retry blokkerer partisjonens consumer thread

### Risiko
- Langvarig databasenedetid (>60s) kan gi tap av mange meldinger — mitigert med alert og manuell re-read fra topic offset

## Aksjonspunkter

- [x] Implementer delt `CommonErrorHandler` med `ExponentialBackOff`
- [x] Implementer `DroppedMessageMeter` med counter + logging
- [x] Fjern per-listener error handlers (NOM, Oppfølging)
- [x] Fjern `runCatching` i NOM-konsument
- [x] Verifiser idempotens i alle konsumenter (INSERT ON CONFLICT DO UPDATE)
- [ ] Sett opp Grafana-alert på `kafka.message.dropped > 0`


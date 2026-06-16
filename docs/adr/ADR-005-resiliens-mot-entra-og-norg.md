# ADR-005: Resiliens mot Entra og NORG

**Dato:** 2026-06-16  
**Status:** Godkjent  
**Beslutningstakere:** Team Sikkerhetstjenesten

## Kontekst

`entra-proxy` er en synkron fasade over eksterne tjenester. Løsningen er derfor sårbar for:

- nettverksfeil
- treghet i nedstrømsystemer
- midlertidige serverfeil
- delvis utilgjengelighet i Entra eller NORG
- uventede svar fra eksterne API-er

Samtidig er ikke alle feil like:

- `404 Not Found` kan være et legitimt domenesvar og skal ikke automatisk retries
- andre `4xx`-feil er normalt klient- eller kontraktsfeil og bør behandles som irrecoverable
- `5xx`-feil og transportfeil kan være midlertidige og bør kunne retries

Dagens løsning har allerede flere mekanismer for dette:

- `DefaultRestErrorHandler` klassifiserer feil i recoverable og irrecoverable kategorier
- `RetryingWhenRecoverable` brukes på tjenestelag mot Entra og NORG
- `PingableHealthIndicator` brukes for å eksponere helse for integrasjonene
- caching reduserer last og skjermer delvis mot kortvarige problemer
- `EntraTjeneste` har eksplisitt fallback for å rydde OID-cache og hente ny OID ved `NotFound`

## Beslutning

Vi bruker en **lagdelt resiliensmodell** for kall mot Entra og NORG.

Denne modellen består av fire deler:

1. **Feilklassifisering** nær HTTP-klienten
2. **Retry** kun for recoverable feil
3. **Health checks** for eksplisitt observabilitet mot nedstrømsystemer
4. **Cache og målrettet fallback** for å redusere effekt av midlertidige problemer og foreldede nøkler

Målet er å være robust mot midlertidige feil uten å skjule reelle kontrakts- eller domeneproblemer.

## Hvorfor dette er valgt

En synkron tjeneste uten differensiert feilbehandling blir enten for skjør eller for aggressiv i retrier.

Denne modellen er valgt fordi den:

- skiller mellom feil det gir mening å prøve igjen og feil det ikke gir mening å prøve igjen
- gjør feilklassifisering eksplisitt og gjenbrukbar
- gir operativ innsikt gjennom helseindikatorer
- kombinerer teknisk retry med domenespesifikk fallback der det faktisk trengs

Eksempler fra dagens løsning:

- `DefaultRestErrorHandler` oversetter `404` til `NotFoundRestException`
- øvrige `4xx` blir `IrrecoverableRestException`
- `5xx` og tilsvarende feil blir `RecoverableRestException`
- `RetryingWhenRecoverable` inkluderer også `ResourceAccessException`
- både Entra og NORG kan pinges via egne health-indikatorer

## Alternativer vurdert

### Alternativ A: Ingen retry, kun ren feilpropagering
- **Fordeler:** Enkel adferd og lite skjult kontrollflyt
- **Ulemper:** Tjenesten blir svært følsom for midlertidige feil i nettverk og nedstrømsystemer
- **Vurdering:** For lite robust for en synkron integrasjonstjeneste

### Alternativ B: Retry på alle feil
- **Fordeler:** Enkelt å konfigurere og kan skjule enkelte midlertidige feil
- **Ulemper:** Risiko for å retrye feil som aldri vil lykkes, økt last på nedstrømsystemer og mer støy i feilsøking
- **Vurdering:** For grovkornet og risikabelt

### Alternativ C: Feilklassifisering + målrettet retry + health checks + fallback (valgt)
- **Fordeler:** Tydelig modell, bedre operasjonell kontroll og mer presis retry-adferd
- **Ulemper:** Flere bevegelige deler og behov for disiplinert vedlikehold av feilklassifiseringen
- **Vurdering:** Beste balanse mellom robusthet og forutsigbarhet

## Arkitektoniske føringer

### Feilklassifisering
- Feil skal klassifiseres nær HTTP-integrasjonslaget
- `404` og andre funksjonelle klientfeil skal ikke retries ukritisk
- Recoverable og irrecoverable feil skal uttrykkes gjennom egne exception-typer

### Retry-strategi
- Retry skal bare brukes for feil som kan være midlertidige
- Retry-regler skal ligge samlet og gjenbrukbart, ikke spres tilfeldig i kodebasen
- Retry skal kombineres med tydelig logging og observabilitet

### Fallback og cache
- Domenespesifikke fallback-mekanismer kan brukes når det finnes en kjent og trygg strategi, som ved fornying av OID
- Cache skal redusere belastning og øke robusthet, men ikke skjule vedvarende feiltilstander

### Operasjonell observabilitet
- Integrasjoner skal kunne rapportere helse eksplisitt
- Helseindikatorer skal peke på relevante ping-endepunkter
- Feil skal gi nok kontekst til å kunne feilsøkes uten å spre integrasjonsdetaljer i hele applikasjonen

## Konsekvenser

### Positive
- Bedre robusthet mot midlertidige feil i Entra og NORG
- Mindre risiko for unødvendige retrier på permanente feil
- Klarere observabilitet rundt integrasjonshelsen
- Mulighet for trygg, domenespesifikk fallback der det er hensiktsmessig

### Negative
- Mer kompleksitet enn en helt flat feilstrategi
- Retry og fallback kan gjøre kontrollflyten mindre åpenbar for nye utviklere
- Feilklassifisering må holdes korrekt over tid når integrasjoner endrer seg

### Risiko
- Feilklassifisering kan bli for grov eller feil dersom nye responstyper ikke vurderes eksplisitt
- For aggressiv retry kan øke last på nedstrømsystemer i feilperioder
- For svak observabilitet kan gjøre det vanskelig å skille cacheeffekter fra reelle integrasjonsfeil

## Aksjonspunkter

- [x] Behold sentral feilklassifisering i `DefaultRestErrorHandler`
- [x] Behold `RetryingWhenRecoverable` som felles retry-abstraksjon for recoverable feil
- [x] Behold health-indikatorer for Entra og NORG
- [x] Behold målrettet fallback for fornying av OID ved `NotFound`
- [ ] Vurder å dokumentere retry-parametere og forventet operasjonell adferd eksplisitt
- [ ] Vurder egne dashboards eller alarmer for health, retry og integrasjonsfeil


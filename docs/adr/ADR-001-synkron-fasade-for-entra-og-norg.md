# ADR-001: Synkron fasade for Entra og NORG med domeneorientert API

**Dato:** 2026-06-16  
**Status:** Godkjent  
**Beslutningstakere:** Team Sikkerhetstjenesten

## Kontekst

`entra-proxy` eksponerer et internt API for å hente:

- ansattes tematilganger
- ansattes enhetstilhørighet
- medlemmer i en enhet eller gruppe
- utvidet informasjon om ansatt
- sikkerhetsgrupper for en ansatt

Kildedataene kommer ikke fra én samlet domeneplattform, men fra flere eksterne systemer:

- Microsoft Entra / Graph for identiteter, grupper og medlemskap
- NORG for oppslag av enhetsnavn

Direkte bruk av Graph fra alle konsumenter ville gitt flere utfordringer:

- lekkasje av eksterne API-detaljer inn i mange konsumenter
- duplisering av spørringslogikk, filteruttrykk og sikkerhetsregler
- ulik håndtering av OBO- og CC-scenarier
- behov for å slå sammen data fra Entra og NORG i hver enkelt konsument

Dagens kode er allerede strukturert rundt denne problemstillingen:

- kontrollerlag i `tilgang`
- domenetjenester i `graph` og `norg`
- adapter/klientlag mot eksterne API-er
- egne verdityper som `AnsattId`, `TIdent`, `Tema` og `Enhetnummer`

## Beslutning

Vi bruker `entra-proxy` som en **synkron fasade** over Entra og NORG, med et **domeneorientert API** for interne brukere.

Arkitekturen organiseres slik:

1. **Kontrollere** eksponerer et stabilt HTTP-API for interne behov
2. **Tjenestelag** samler domenelogikk og skjuler eksterne detaljer
3. **Adaptere og klienter** kapsler kommunikasjon med Entra og NORG
4. **Domeneobjekter og verdityper** brukes i interne kontrakter fremfor rå eksterne DTO-er

`EntraTjeneste` er hovedinngangen for forretningsnære oppslag mot Entra, mens `NorgTjeneste` brukes som støtteintegrasjon for berikelse av enhetsdata.

## Hvorfor dette er valgt

Denne løsningen gir en tydelig ansvarsdeling:

- kontrollerne uttrykker brukstilfeller
- tjenestene uttrykker domeneatferd
- adaptere og klienter uttrykker teknisk integrasjon

Det gjør det også mulig å tilby enklere og mer stabile kontrakter enn de eksterne API-ene.

Eksempler fra dagens løsning:

- `EntraController` skiller mellom produksjons-API og tokenkrav
- `DevEntraController` gir et separat testvennlig dev-endepunkt
- `EntraTjeneste` kombinerer Entra-data med navn fra NORG
- `EntraConfig` eier Graph-spesifikke URI-byggere og filteruttrykk

## Alternativer vurdert

### Alternativ A: La konsumenter kalle Graph direkte
- **Fordeler:** Færre interne komponenter
- **Ulemper:** Mange konsumenter må lære Graph, tokenflyt og filterlogikk. Det blir også vanskeligere å styre endringer sentralt
- **Vurdering:** Avvist fordi det sprer integrasjonsansvar ut i organisasjonen

### Alternativ B: Synkron fasade med domenetjenester (valgt)
- **Fordeler:** Tydelige kontrakter, sentralisert sikkerhet, skjult integrasjonskompleksitet, enklere berikelse fra flere kilder
- **Ulemper:** Enda et tjenestelag å drifte og overvåke
- **Vurdering:** Beste balanse mellom enkel konsumering og kontroll over integrasjonen

### Alternativ C: Materialisere data i egen database og servere alt derfra
- **Fordeler:** Kan gi lav latenstid og redusert kobling til kildesystemer i lesestien
- **Ulemper:** Høyere kompleksitet, behov for synkronisering og eierskap til en kopi av identitetsdata
- **Vurdering:** Ikke nødvendig for dagens behov

## Arkitektoniske føringer

### API-design
- Interne API-er skal uttrykke NAV-domene, ikke Graph-domene
- Verdityper skal brukes for identifikatorer og sentrale begreper
- Berikelse fra flere kilder skjer i tjenestelaget, ikke i kontrollerne

### Integrasjonsgrenser
- URI-bygging, filteruttrykk og eksterne headere holdes i konfigurasjon og adapterlag
- Eksterne DTO-er holdes nær adaptere og klienter
- Feilhåndtering og retry håndteres sentralt i integrasjonslagene

### Operasjonelle hensyn
- Løsningen er synkron og derfor avhengig av tilgjengelighet i nedstrømsystemene
- Arkitekturen må derfor kombineres med caching, helseendepunkter og resiliensmekanismer

## Konsekvenser

### Positive
- Konsumenter får et enklere og mer stabilt API
- Eksterne API-detaljer holdes samlet ett sted
- Berikelse med NORG-data kan gjøres konsistent
- Sikkerhet og tokenlogikk kan håndheves sentralt
- Domeneobjekter gjør koden mer lesbar og lettere å vedlikeholde

### Negative
- Tjenesten blir en kritisk mellomkomponent
- Den synkrone kjeden gir sensitivitet for feil og treghet i Entra eller NORG
- Nye behov kan presse flere ansvar inn i fasaden hvis grensene ikke holdes tydelige

### Risiko
- Fasaden kan gli over i å bli en generell «alt-mulig-tjeneste» for Entra dersom nye endepunkter legges til uten et tydelig domeneformål
- For mye domenelogikk i kontrollere eller adaptere vil svekke lagdelingen

## Aksjonspunkter

- [x] Behold domenetjenester som inngang til forretningslogikk
- [x] Behold adaptere/klienter som eneste sted for eksterne kall
- [x] Behold verdityper i API og tjenestelag
- [ ] Vurder å dokumentere ønsket pakkestruktur eksplisitt i utviklerdokumentasjon
- [ ] Vurder å dokumentere eksterne avhengigheter og dataflyt med et enkelt systemdiagram



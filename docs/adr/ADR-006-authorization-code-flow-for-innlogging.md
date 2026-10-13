# ADR-006: Authorization Code Flow for interaktiv innlogging

**Dato:** 2026-10-13  
**Status:** Godkjent  
**Beslutningstakere:** Team Sikkerhetstjenesten

## Kontekst

`entra-proxy` har til nå kun støttet to tokendrevne sikkerhetskontekster
(se [ADR-004](ADR-004-sikkerhetsmodell-for-obo-og-cc.md)):

1. **On-behalf-of (OBO)** – kall utført for en innlogget ansatt representert i et innkommende token
2. **Client credentials (CC)** – system-til-system-kall uten sluttbruker

Begge forutsetter at kalleren allerede har et gyldig Azure AD-token. Tjenesten
har ikke hatt en egen interaktiv innlogging der en ansatt logger seg inn direkte
mot `entra-proxy` i nettleseren (for eksempel for utforsker-/ansattsider eller
Swagger-UI).

## Beslutning

Vi legger til **Authorization Code Flow** mot Azure AD for interaktiv
nettleserinnlogging, i tillegg til dagens OBO- og CC-modeller.

Dette innebærer:

- en egen klientregistrering `entra` med `authorization_code` som grant-type,
  `redirect-uri` satt til `{baseUrl}/login/oauth2/code/{registrationId}` og
  scopene `openid` og `profile`
- en dedikert `SecurityFilterChain` (`@Order(1)`) som matcher
  innloggingsendepunktene (`/oauth2/authorization/**`, `/login/oauth2/code/**`,
  `/logout`) og den beskyttede siden `/utforsker.html`, og aktiverer
  `oauth2Login`
- `/utforsker.html` er foreløpig den eneste siden som krever interaktiv
  innlogging; øvrige sider og API-endepunkter er uendret
- den eksisterende resource-server-kjeden beholdes uendret som `@Order(2)` og er
  fortsatt stateless for `/api/**`
- `OAuth2AuthorizedClientManager` utvides med `authorizationCode`- og
  `refreshToken`-providere slik at tokens fra en innlogget bruker kan brukes og
  fornyes mot nedstrøms tjenester
- NAIS-manifestet får `replyURLs` for `/login/oauth2/code/entra` per ingress

## Hvorfor dette er valgt

- Innlogging legges i en egen, eksplisitt filterkjede slik at
  resource-server-modellen (OBO/CC) forblir uendret og stateless.
- Skillet mellom innlogging og API-trafikk gjør sikkerhetsreglene lette å lese
  og teste, i tråd med føringene i ADR-004.
- Gjenbruk av eksisterende `azuread`-provider og app-registrering holder
  konfigurasjonen enkel.

## Alternativer vurdert

### Alternativ A: Interaktiv innlogging i samme filterkjede som resource server
- **Ulemper:** Krever sesjon og bryter den stateless resource-server-modellen
- **Vurdering:** Avvist

### Alternativ B: Egen ordnet filterkjede for innlogging (valgt)
- **Fordeler:** Isolerer sesjonsbasert innlogging fra stateless API-trafikk
- **Vurdering:** Beste balanse mellom tydelighet og minimal påvirkning

### Alternativ C: Sidecar (Wonderwall) for innlogging
- **Ulemper:** Mer infrastruktur enn nødvendig for dagens behov
- **Vurdering:** Ikke valgt nå, kan revurderes ved behov

## Konsekvenser

### Positive
- Ansatte kan logge seg inn interaktivt mot `entra-proxy`
- Resource-server-modellen for OBO og CC er uendret

### Negative
- Innloggingskjeden er sesjonsbasert og krever `replyURLs` registrert i Azure AD
- Flere klientregistreringer å vedlikeholde

### Risiko
- Feil eller manglende `replyURLs` gir innloggingsfeil i et miljø

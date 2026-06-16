# Architecture Decision Records (ADR)

Arkitekturbeslutninger for populasjonstilgangskontroll, dokumentert etter [Architecture Advice Process](https://sikkerhet.nav.no).

## Oversikt

| # | Beslutning | Status | Dato |
|---|-----------|--------|------|
| [ADR-001](ADR-001-regelmotor-arkitektur.md) | Regelmotor med interface-baserte regler og Spring-komponentskanning | Godkjent | 2025-01-15 |
| [ADR-002](ADR-002-caching-strategi.md) | Valkey (Redis) for distribuert cache med resilient feilhåndtering | Godkjent | 2025-03-01 |
| [ADR-003](ADR-003-kafka-feilhåndtering.md) | Eksponentiell backoff uten DLT for Kafka-konsumenter | Godkjent | 2025-06-01 |
| [ADR-004](ADR-004-enkelttilgang-overstyring.md) | Tidsbegrenset overstyring av regelbrudd via enkelttilgang | Godkjent | 2025-01-15 |
| [ADR-005](ADR-005-domenemodell-bruker-ansatt.md) | Separasjon av Bruker- og Ansatt-domene med ekstern databerikelse | Godkjent | 2025-01-15 |
| [ADR-006](ADR-006-gradle-versjonskatalog.md) | Gradle version catalog for avhengigheter og plugins | Godkjent | 2026-06-16 |
| [ADR-007](ADR-007-gjenbrukbare-github-workflows.md) | Gjenbrukbare GitHub Actions-workflows for build og deploy | Godkjent | 2026-06-16 |


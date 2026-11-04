package no.nav.sikkerhetstjenesten.entraproxy.graph

import no.nav.sikkerhetstjenesten.felles.rest.PingableHealthIndicator
import org.springframework.boot.health.contributor.HealthIndicator
import org.springframework.stereotype.Component

@Component
class EntraHealthIndicator(cfg: EntraConfig, client: EntraGraphClient) :
    HealthIndicator by PingableHealthIndicator(cfg, client::ping)

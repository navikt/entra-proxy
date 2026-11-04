package no.nav.sikkerhetstjenesten.entraproxy.norg

import no.nav.sikkerhetstjenesten.felles.rest.PingableHealthIndicator
import org.springframework.boot.health.contributor.HealthIndicator
import org.springframework.stereotype.Component

@Component
class NorgHealthIndicator(cfg: NorgConfig, client: NorgProxyClient) :
    HealthIndicator by PingableHealthIndicator(cfg, client::ping)

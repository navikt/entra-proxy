package no.nav.sikkerhetstjenesten.entraproxy.felles.cache

import io.micrometer.core.instrument.MeterRegistry
import no.nav.sikkerhetstjenesten.entraproxy.felles.AbstractTeller
import no.nav.sikkerhetstjenesten.entraproxy.felles.rest.AuthContext
import org.springframework.stereotype.Component

@Component
class CacheOppfriskerTeller(registry: MeterRegistry, token: AuthContext) :
    AbstractTeller(registry, token, "cache.oppfrisker", "Antall oppfriskninger av cache etter utløp")
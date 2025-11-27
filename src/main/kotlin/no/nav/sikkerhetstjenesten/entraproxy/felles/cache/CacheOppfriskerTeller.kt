package no.nav.sikkerhetstjenesten.entraproxy.felles.cache

import io.micrometer.core.instrument.MeterRegistry
import no.nav.sikkerhetstjenesten.entraproxy.felles.AbstractTeller
import no.nav.sikkerhetstjenesten.entraproxy.felles.rest.Token
import org.springframework.stereotype.Component

@Component
class CacheOppfriskerTeller(registry: MeterRegistry, token: Token) :
    AbstractTeller(registry, token, "cache.oppfrisker", "Antall oppfriskninger av cache etter utløp")

@Component
class CacheStørrelseTeller(registry: MeterRegistry, token: Token) :
    AbstractTeller(registry, token, "cache.size", "Cache størrelse")
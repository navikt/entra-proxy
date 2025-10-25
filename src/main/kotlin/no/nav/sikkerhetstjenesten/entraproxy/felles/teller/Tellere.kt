package no.nav.sikkerhetstjenesten.entraproxy.felles.teller

import io.micrometer.core.instrument.MeterRegistry
import no.nav.sikkerhetstjenesten.entraproxy.felles.AbstractTeller
import no.nav.sikkerhetstjenesten.entraproxy.felles.rest.Token
import org.springframework.stereotype.Component



@Component
class CacheOppfriskerTeller(registry: MeterRegistry, token: Token) :
    AbstractTeller(registry, token, "cache.oppfrisker", "Antall oppfriskninger av cache etter utløp")

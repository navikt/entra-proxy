package no.nav.sikkerhetstjenesten.entraproxy.felles.rest

import io.micrometer.core.instrument.MeterRegistry
import org.springframework.stereotype.Component

@Component
class TokenTypeTeller(registry: MeterRegistry, token: AuthContext) :
    AbstractTeller(registry, token, "token.type", "Token type")
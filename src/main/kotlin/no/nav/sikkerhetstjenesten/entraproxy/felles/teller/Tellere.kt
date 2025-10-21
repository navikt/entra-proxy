package no.nav.sikkerhetstjenesten.entraproxy.felles.teller

import io.micrometer.core.instrument.MeterRegistry
import no.nav.sikkerhetstjenesten.entraproxy.felles.AbstractTeller
import no.nav.sikkerhetstjenesten.entraproxy.tilgang.Token
import org.springframework.stereotype.Component



@Component
class NasjonalGruppeTeller(registry: MeterRegistry, token: Token) :
    AbstractTeller(registry, token, "gruppe.medlemskap.nasjonal", "Ansatte med og uten nasjonalt gruppemedlemsskap")

@Component
class AvdødTeller(registry: MeterRegistry, accessor: Token) :
    AbstractTeller(registry, accessor, "dead.oppslag.total", "Forsøk på å slå opp avdøde")

@Component
class OverstyringTeller(registry: MeterRegistry, token: Token) :
    AbstractTeller(registry, token, "overstyring.forsøk", "Overstyringsforsøk pr resultat")

@Component
class CacheOppfriskerTeller(registry: MeterRegistry, token: Token) :
    AbstractTeller(registry, token, "cache.oppfrisker", "Antall oppfriskninger av cache etter utløp")

@Component
class BulkCacheSuksessTeller(registry: MeterRegistry, token: Token) :
    AbstractTeller(registry, token, "bulk.cache.suksess", "Hits/misses for entire bulk")

@Component
class TokenTypeTeller(registry: MeterRegistry, token: Token) :
    AbstractTeller(registry, token, "token.type", "Token type")

package no.nav.sikkerhetstjenesten.entraproxy.felles.rest

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Tags

abstract class AbstractTeller(private val registry: MeterRegistry, private val token: Token, private val navn: String, private val beskrivelse: String) {

    open fun tell(tags: Tags = Tags.empty(), n:Int=1) =
        Counter.builder(navn)
            .description(beskrivelse)
            .tags(tags
                .and("token", token.type)
                .and("system", token.system)
                .and("clustersystem", token.clusterAndSystem))
            .register(registry)
            .increment(n.toDouble())
}
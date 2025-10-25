package no.nav.sikkerhetstjenesten.entraproxy.graph

import io.micrometer.core.annotation.Timed
import io.opentelemetry.instrumentation.annotations.WithSpan
import no.nav.sikkerhetstjenesten.entraproxy.felles.rest.RetryingWhenRecoverable
import no.nav.sikkerhetstjenesten.entraproxy.graph.EntraConfig.Companion.GRAPH
import no.nav.sikkerhetstjenesten.entraproxy.norg.NorgTjeneste
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import java.util.*

@RetryingWhenRecoverable
@Service
@Timed(value = "entra", histogram = true)
class EntraTjeneste(private val adapter: EntraRestClientAdapter, private val norgTjeneste: NorgTjeneste)  {

    @Cacheable(cacheNames = [GRAPH],  key = "#root.methodName + ':' + #ansattId.verdi")
    @WithSpan
    fun enheter(ansattId: AnsattId, oid: UUID): Set<Enhet> = buildSet {
        adapter.enheter("$oid").forEach { enhetsnummer ->
            add(Enhet(enhetsnummer, norgTjeneste.navnFor(enhetsnummer)))
        }
    }

    @WithSpan
    @Cacheable(cacheNames = [GRAPH],  key = "#root.methodName + ':' + #ansattId.verdi")
    fun tema(ansattId: AnsattId, oid: UUID) = adapter.tema("$oid")

    override fun toString() = "${javaClass.simpleName} [adapter=$adapter norgTjeneste=$norgTjeneste]"
}



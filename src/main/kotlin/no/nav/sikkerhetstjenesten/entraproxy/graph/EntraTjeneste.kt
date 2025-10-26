package no.nav.sikkerhetstjenesten.entraproxy.graph

import io.micrometer.core.annotation.Timed
import io.opentelemetry.instrumentation.annotations.WithSpan
import no.nav.sikkerhetstjenesten.entraproxy.felles.rest.RetryingWhenRecoverable
import no.nav.sikkerhetstjenesten.entraproxy.graph.Enhet.Enhetnummer
import no.nav.sikkerhetstjenesten.entraproxy.graph.EntraConfig.Companion.GRAPH
import no.nav.sikkerhetstjenesten.entraproxy.norg.NorgTjeneste
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import java.util.*

@RetryingWhenRecoverable
@Service
@Timed(value = GRAPH, histogram = true)
class EntraTjeneste(private val adapter: EntraRestClientAdapter, private val norgTjeneste: NorgTjeneste)  {

    @Cacheable(cacheNames = [GRAPH],  key = "#root.methodName + ':' + #ansattId.verdi")
    @WithSpan
    fun enheter(ansattId: AnsattId, oid: UUID): Set<Enhet> =
        buildSet {
            adapter.enheter("$oid").forEach {
                add(Enhet(it, norgTjeneste.navnFor(it)))
            }
        }

    @Cacheable(cacheNames = [GRAPH],  key = "#root.methodName + ':' + #enhet.verdi")
    @WithSpan
    fun enhetMedlemmer(enhet: Enhetnummer, oid: UUID) =
        UnsupportedOperationException("Henting av enhet medlemmer er ikke implementert")

    @Cacheable(cacheNames = [GRAPH],  key = "#root.methodName + ':' + #tema.verdi")
    @WithSpan
    fun temaMedlemmer( tema: Tema, oid: UUID) =
        UnsupportedOperationException("Henting av tema medlemmer er ikke implementert")

    @WithSpan
    @Cacheable(cacheNames = [GRAPH],  key = "#root.methodName + ':' + #ansattId.verdi")
    fun tema(ansattId: AnsattId, oid: UUID) =
        adapter.tema("$oid")

    override fun toString() = "${javaClass.simpleName} [adapter=$adapter, norgTjeneste=$norgTjeneste]"
}



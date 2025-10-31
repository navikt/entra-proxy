package no.nav.sikkerhetstjenesten.entraproxy.graph

import io.micrometer.core.annotation.Timed
import io.opentelemetry.instrumentation.annotations.WithSpan
import no.nav.sikkerhetstjenesten.entraproxy.felles.rest.GruppeIdCachableRestConfig.Companion.GRUPPEID
import no.nav.sikkerhetstjenesten.entraproxy.felles.rest.MedlemmerCachableRestConfig.Companion.MEDLEMMER
import no.nav.sikkerhetstjenesten.entraproxy.felles.rest.RetryingWhenRecoverable
import no.nav.sikkerhetstjenesten.entraproxy.graph.Enhet.Enhetnummer
import no.nav.sikkerhetstjenesten.entraproxy.graph.EntraConfig.Companion.ENHET_PREFIX
import no.nav.sikkerhetstjenesten.entraproxy.graph.EntraConfig.Companion.GRAPH
import no.nav.sikkerhetstjenesten.entraproxy.graph.EntraConfig.Companion.TEMA_PREFIX
import no.nav.sikkerhetstjenesten.entraproxy.norg.NorgTjeneste
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import java.util.*

@RetryingWhenRecoverable
@Service
@Timed(value = GRAPH, histogram = true)
class EntraTjeneste(private val adapter: EntraRestClientAdapter, private val norgTjeneste: NorgTjeneste)  {


    @WithSpan
    @Cacheable(cacheNames = [GRAPH],  key = "#root.methodName + ':' + #ansattId.verdi")
    fun tema(ansattId: AnsattId, oid: UUID) =
        adapter.tema("$oid")

    @Cacheable(cacheNames = [GRAPH],  key = "#root.methodName + ':' + #ansattId.verdi")
    @WithSpan
    fun enheter(ansattId: AnsattId, oid: UUID): Set<Enhet> =
        buildSet {
            adapter.enheter("$oid").forEach {
                add(Enhet(it, norgTjeneste.navnFor(it)))
            }
        }

    @WithSpan
    @Cacheable(cacheNames = [MEDLEMMER])
    fun medlemmer(gruppeId: UUID) : Set<AnsattId> =
            adapter.medlemmer(gruppeId.toString())

    @Cacheable(cacheNames = [GRUPPEID],  key = "$TEMA_PREFIX + #tema.verdi")
    @WithSpan
    fun gruppeIdForTema( tema: Tema) =
        adapter.gruppeId(TEMA_PREFIX + tema.verdi)

    @Cacheable(cacheNames = [GRUPPEID],  key = "$ENHET_PREFIX + #enhet.verdi")
    @WithSpan
    fun gruppeIdForEnhet( enhet: Enhetnummer) =
        adapter.gruppeId(ENHET_PREFIX + enhet.verdi)



    override fun toString() = "${javaClass.simpleName} [adapter=$adapter, norgTjeneste=$norgTjeneste]"
}



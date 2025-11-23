package no.nav.sikkerhetstjenesten.entraproxy.graph

import io.micrometer.core.annotation.Timed
import io.opentelemetry.instrumentation.annotations.WithSpan
import no.nav.sikkerhetstjenesten.entraproxy.felles.rest.MedlemmerCachableRestConfig.Companion.MEDLEMMER
import no.nav.sikkerhetstjenesten.entraproxy.felles.rest.RetryingWhenRecoverable
import no.nav.sikkerhetstjenesten.entraproxy.felles.utils.extensions.TimeExtensions.tidOgLog
import no.nav.sikkerhetstjenesten.entraproxy.graph.EntraConfig.Companion.GRAPH
import no.nav.sikkerhetstjenesten.entraproxy.norg.NorgTjeneste
import org.slf4j.LoggerFactory.getLogger
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import java.util.*

@RetryingWhenRecoverable
@Service
@Timed(value = GRAPH, histogram = true)
class EntraTjeneste(private val adapter: EntraRestClientAdapter, private val norg: NorgTjeneste)  {

    private val log = getLogger(javaClass)

    @WithSpan
    @Cacheable(cacheNames = [GRAPH],  key = "#root.methodName + ':' + #ansattId.verdi")
    fun tema(ansattId: AnsattId, oid: UUID) =
        tidOgLog(log, "tema for $ansattId") {
            adapter.tema("$oid")
        }

    @WithSpan
    @Cacheable(cacheNames = [GRAPH],  key = "#root.methodName + ':' + #ansattId.verdi")
    fun enheter(ansattId: AnsattId, oid: UUID)  =
        tidOgLog(log, "enheter for $ansattId") {
            buildSet {
                adapter.enheter("$oid").forEach {
                    add(Enhet(it, norg.navnFor(it)))
                }
            }
        }

    @WithSpan
    @Cacheable(MEDLEMMER)
    fun medlemmer(gruppeId: UUID) =
        tidOgLog(log, "medlemmer for gruppe $gruppeId") {
            adapter.gruppeMedlemmer("$gruppeId")
        }

    @WithSpan
    fun ansattUtvidet(navIdent: String) =
        adapter.ansattUtvidet(navIdent)

    override fun toString() =
        "${javaClass.simpleName} [adapter=$adapter, norg=$norg]"
}


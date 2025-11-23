package no.nav.sikkerhetstjenesten.entraproxy.graph

import io.micrometer.core.annotation.Timed
import io.opentelemetry.instrumentation.annotations.WithSpan
import no.nav.sikkerhetstjenesten.entraproxy.felles.rest.MedlemmerCachableRestConfig.Companion.MEDLEMMER
import no.nav.sikkerhetstjenesten.entraproxy.felles.rest.RetryingWhenRecoverable
import no.nav.sikkerhetstjenesten.entraproxy.graph.EntraConfig.Companion.GRAPH
import no.nav.sikkerhetstjenesten.entraproxy.norg.NorgTjeneste
import org.slf4j.LoggerFactory.getLogger
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import java.util.*
import kotlin.time.measureTimedValue

@RetryingWhenRecoverable
@Service
@Timed(value = GRAPH, histogram = true)
class EntraTjeneste(private val adapter: EntraRestClientAdapter, private val norg: NorgTjeneste)  {

    private val log = getLogger(javaClass)

    @WithSpan
    @Cacheable(cacheNames = [GRAPH],  key = "#root.methodName + ':' + #ansattId.verdi")
    fun tema(ansattId: AnsattId, oid: UUID) =
        tidOgLog("tema for $ansattId") {
            adapter.tema("$oid")
        }

    @WithSpan
    @Cacheable(cacheNames = [GRAPH],  key = "#root.methodName + ':' + #ansattId.verdi")
    fun enheter(ansattId: AnsattId, oid: UUID)  =
        tidOgLog("enheter for $ansattId")  {
            buildSet {
                adapter.enheter("$oid").forEach {
                    add(Enhet(it,norg.navnFor(it)))
                }
            }
        }

    @WithSpan
    @Cacheable(MEDLEMMER)
    fun medlemmer(gruppeId: UUID) =
        tidOgLog("medlemmer for gruppe $gruppeId") {
            adapter.gruppeMedlemmer("$gruppeId")
        }

    private inline fun <T> tidOgLog(type: String, block: () -> Set<T>) =
        measureTimedValue {
            block()
        }.let {
            log.info("Hentet ${it.value.size} $type på ${it.duration.inWholeMilliseconds}ms")
            it.value}


    @WithSpan
    fun ansattUtvidet(navIdent: String) =
        adapter.ansattUtvidet(navIdent)

    override fun toString() =
        "${javaClass.simpleName} [adapter=$adapter, norg=$norg]"
}


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
import kotlin.system.measureTimeMillis
import kotlin.time.DurationUnit
import kotlin.time.DurationUnit.MILLISECONDS
import kotlin.time.TimedValue
import kotlin.time.measureTimedValue
import kotlin.time.toDuration

@RetryingWhenRecoverable
@Service
@Timed(value = GRAPH, histogram = true)
class EntraTjeneste(private val adapter: EntraRestClientAdapter, private val norg: NorgTjeneste)  {

    private val log = getLogger(javaClass)

    @WithSpan
    @Cacheable(cacheNames = [GRAPH],  key = "#root.methodName + ':' + #ansattId.verdi")
    fun tema(ansattId: AnsattId, oid: UUID) =
        measureTimedValue {
            adapter.tema("$oid")
        }.let { timed ->
            log.info("Hentet ${timed.value.size} tema for $ansattId")
            timed.value
        }

    @WithSpan
    @Cacheable(cacheNames = [GRAPH],  key = "#root.methodName + ':' + #ansattId.verdi")
    fun enheter(ansattId: AnsattId, oid: UUID)  =
        measureTimedValue {
            buildSet {
                adapter.enheter("$oid").forEach {
                    add(Enhet(it,norg.navnFor(it)))
                }
            }
        }.let { timed ->
            log.info("Hentet ${timed.value.size} enheter for ansatt ${ansattId} og gruppe $oid på ${timed.duration.inWholeMilliseconds} ms")
            timed.value
        }

    @WithSpan
    @Cacheable(MEDLEMMER)
    fun medlemmer(gruppeId: UUID) = measureTimedValue {
        adapter.gruppeMedlemmer("$gruppeId")
    }.let { timed ->
        log.info("Hentet ${timed.value.size} medlemmer for gruppe $gruppeId på ${timed.duration.inWholeMilliseconds} ms")
        timed.value
    }
    
    @WithSpan
    fun ansattUtvidet(navIdent: String) =
        adapter.ansattUtvidet(navIdent)

    override fun toString() =
        "${javaClass.simpleName} [adapter=$adapter, norg=$norg]"
}


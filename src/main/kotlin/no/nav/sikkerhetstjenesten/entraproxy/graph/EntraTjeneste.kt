package no.nav.sikkerhetstjenesten.entraproxy.graph

import io.micrometer.core.annotation.Timed
import io.opentelemetry.instrumentation.annotations.WithSpan
import no.nav.sikkerhetstjenesten.entraproxy.felles.rest.RetryingWhenRecoverable
import no.nav.sikkerhetstjenesten.entraproxy.graph.Enhet.Enhetnummer
import no.nav.sikkerhetstjenesten.entraproxy.graph.EntraConfig.Companion.ENHET_PREFIX
import no.nav.sikkerhetstjenesten.entraproxy.graph.EntraConfig.Companion.GRAPH
import no.nav.sikkerhetstjenesten.entraproxy.graph.EntraConfig.Companion.TEMA_PREFIX
import no.nav.sikkerhetstjenesten.entraproxy.norg.NorgTjeneste
import org.slf4j.LoggerFactory.getLogger
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import java.util.*
import kotlin.toString

@RetryingWhenRecoverable
@Service
@Timed(value = GRAPH, histogram = true)
class EntraTjeneste(private val adapter: EntraRestClientAdapter, private val norgTjeneste: NorgTjeneste)  {

    private val log = getLogger(javaClass)

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
    fun enhetMedlemmer(enhet: Enhetnummer, gruppeId: UUID) = buildSet {
        "$gruppeId".let {
            adapter.medlemmer(it).also {
                log.info("Enhet $enhet med gruppeId $gruppeId har medlemmer: $it")
            }.forEach { navIdent -> add(AnsattId(navIdent)) }
        }
    }
    @Cacheable(cacheNames = [GRAPH],  key = "#root.methodName + ':' + #tema.verdi")
    @WithSpan
    fun temaMedlemmer(tema: Tema, gruppeId: UUID) = buildSet {
        "$gruppeId".let {
            adapter.medlemmer(it)
                .also {
                    log.info("Tema $tema med gruppeId $gruppeId har medlemmer: $it")
                }
                .forEach { navIdent -> add(AnsattId(navIdent)) }
        }
    }
    @Cacheable(cacheNames = [GRAPH],  key = "#root.methodName + ':' + #tema.verdi")
    @WithSpan
    fun gruppeIdForTema( tema: Tema) =
        adapter.gruppeId(TEMA_PREFIX + tema.verdi)

    @Cacheable(cacheNames = [GRAPH],  key = "#root.methodName + ':' + #enhet.verdi")
    @WithSpan
    fun gruppeIdForEnhet( enhet: Enhetnummer) =
        adapter.gruppeId(ENHET_PREFIX + enhet.verdi)

    @WithSpan
    @Cacheable(cacheNames = [GRAPH],  key = "#root.methodName + ':' + #ansattId.verdi")
    fun tema(ansattId: AnsattId, oid: UUID) =
        adapter.tema("$oid")

    override fun toString() = "${javaClass.simpleName} [adapter=$adapter, norgTjeneste=$norgTjeneste]"
}



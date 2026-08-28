package no.nav.sikkerhetstjenesten.entraproxy.graph

import no.nav.sikkerhetstjenesten.entraproxy.felles.cache.CacheOperations
import no.nav.sikkerhetstjenesten.entraproxy.felles.rest.NotFoundRestException
import no.nav.sikkerhetstjenesten.entraproxy.graph.MedlemmerCachableRestConfig.Companion.MEDLEMMER
import no.nav.sikkerhetstjenesten.entraproxy.felles.rest.RetryingWhenRecoverableService
import no.nav.sikkerhetstjenesten.entraproxy.felles.utils.extensions.TimeExtensions.tidOgLog
import no.nav.sikkerhetstjenesten.entraproxy.graph.EntraConfig.Companion.GRAPH
import no.nav.sikkerhetstjenesten.entraproxy.graph.EntraOidCachableRestConfig.Companion.ANSATT_OID_CACHE
import no.nav.sikkerhetstjenesten.entraproxy.norg.NorgTjeneste
import org.slf4j.LoggerFactory.getLogger
import org.springframework.cache.annotation.Cacheable
import java.util.*
import kotlin.time.measureTimedValue

@RetryingWhenRecoverableService
class EntraTjeneste(private val adapter: EntraRestClientAdapter, private val norg: NorgTjeneste, private val oid: EntraOidTjeneste, private val cache: CacheOperations)  {

    private val log = getLogger(javaClass)

    @Cacheable(cacheNames = [GRAPH], key = "#root.methodName + ':' + #ansattId.verdi")
    fun tema(ansattId: AnsattId, oid: UUID): Set<Tema> {
        val (result, duration) = measureTimedValue {
            medNotFoundFallback(oid, {
                adapter.tema("$it")
            }) {
                refreshOid(ansattId)
            }
        }
        log.info("Hentet ${result.size} ${"tema for $ansattId"} på ${duration.inWholeMilliseconds}ms")
        return result
    }


    @Cacheable(cacheNames = [GRAPH], key = "#root.methodName + ':' + #ansattId.verdi")
    fun enheter(ansattId: AnsattId, oid: UUID): Set<Enhet> {
        val (result, duration) = measureTimedValue {
            medNotFoundFallback(oid, ::enheter) {
                refreshOid(ansattId)
            }
        }
        log.info("Hentet ${result.size} ${"enhet(er) for $ansattId"} på ${duration.inWholeMilliseconds}ms")
        return result
    }


    @Cacheable(MEDLEMMER)
    fun medlemmer(gruppeId: UUID): Set<Ansatt> {
        val (result, duration) = measureTimedValue {
            adapter.gruppeMedlemmer("$gruppeId")
        }
        log.info("Hentet ${result.size} ${"medlem(mer) for gruppe $gruppeId"} på ${duration.inWholeMilliseconds}ms")
        return result
    }

    @Cacheable(cacheNames = [GRAPH], key = "#root.methodName + ':' + #ansattId.verdi")
    fun utvidetAnsatt(ansattId: AnsattId) =
        ansatt  {
            adapter.utvidetAnsatt(ansattId.verdi)
        }

    @Cacheable(cacheNames = [GRAPH], key = "#root.methodName + ':' + #ansattId.verdi")
    fun utvidetAnsatt(ansattId: TIdent) =
        ansatt  {
            adapter.utvidetAnsattTident(ansattId.verdi)
        }


    @Cacheable(cacheNames = [GRAPH], key = "#root.methodName + ':' + #navIdent")
    fun grupperForAnsatt(navIdent: AnsattId, oid: UUID) =
        tidOgLog(log) {
            medNotFoundFallback(oid, { adapter.ansatteGrupper(it.toString()) }) { refreshOid(navIdent) }
        }

    private inline fun <T> medNotFoundFallback(arg: UUID, main: (UUID) -> T, nyOid: (UUID) -> UUID)
            = runCatching { main(arg) }.getOrElse {
        if (it is NotFoundRestException) {
            main(nyOid(arg))
        } else{
            throw it
        }
    }

    private fun enheter(oid: UUID) =
        buildSet {
            adapter.enheter("$oid").forEach {
                add(Enhet(it, norg.navnFor(it)))
            }
        }


    private fun ansatt(block: () -> UtvidetAnsatt?) =
        tidOgLog(log) { block() }

    private fun refreshOid(navIdent: AnsattId): UUID {
        cache.delete(ANSATT_OID_CACHE,navIdent.verdi).also {
            log.info("Slettet cache innslag før henting av ny oid $navIdent")
        }
        return oid.ansattOid(navIdent).also {
            log.info("Hentet  ny oid $it for $navIdent")
        }
            ?: throw NotFoundRestException(adapter.baseURI, "Fant ikke oid for ${navIdent.verdi} i Entra, selv etter cache-opprydding")
    }

    override fun toString() =
        "${javaClass.simpleName} [adapter=$adapter, norg=$norg]"
}

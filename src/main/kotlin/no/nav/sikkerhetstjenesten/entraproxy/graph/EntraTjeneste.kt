package no.nav.sikkerhetstjenesten.entraproxy.graph

import no.nav.sikkerhetstjenesten.entraproxy.cache.CacheInaktiveNavIdenter.Companion.INAKTIVE
import no.nav.sikkerhetstjenesten.entraproxy.graph.EntraGraphClient.Companion.GRAPH
import no.nav.sikkerhetstjenesten.entraproxy.graph.EntraOidConfig.Companion.OID_CACHE
import no.nav.sikkerhetstjenesten.entraproxy.graph.MedlemmerConfig.Companion.MEDLEMMER
import no.nav.sikkerhetstjenesten.felles.cache.CacheOperations
import no.nav.sikkerhetstjenesten.felles.rest.DownstreamURIContext.currentUri
import no.nav.sikkerhetstjenesten.felles.rest.NotFoundRestException
import no.nav.sikkerhetstjenesten.felles.rest.RestRetryingWhenRecoverableService
import org.slf4j.LoggerFactory.getLogger
import org.springframework.cache.annotation.Cacheable
import java.util.UUID

@RestRetryingWhenRecoverableService
class EntraTjeneste(private val graph: EntraGraphOperations, private val cache: CacheOperations, private val oid: EntraOidTjeneste)  {

    private val log = getLogger(javaClass)

    @Cacheable(cacheNames = [GRAPH],  key = "#root.methodName + ':' + #ansattId.verdi")
    fun tema(ansattId: AnsattId, oid: UUID) =
        if (erInaktiv(ansattId)) emptySet()
        else medOidOppfrisking(ansattId, oid) { graph.temaer(it) }

    @Cacheable(cacheNames = [GRAPH],  key = "#root.methodName + ':' + #ansattId.verdi")
    fun enheter(ansattId: AnsattId, oid: UUID) =
        if (erInaktiv(ansattId)) emptySet()
        else medOidOppfrisking(ansattId, oid) { graph.enheter(it) }

    @Cacheable(MEDLEMMER, key = "#gruppeId.toString()")
    fun medlemmerIGruppe(gruppeNavn: String, gruppeId: UUID) =
        graph.gruppeMedlemmer("$gruppeId", gruppeNavn)

    @Cacheable(GRAPH,key = "#root.methodName + ':' + #ansattId.verdi")
    fun utvidetAnsatt(ansattId: AnsattId) =
        graph.utvidetAnsatt(ansattId)

    @Cacheable(GRAPH,key = "#root.methodName + ':' + #ansattId.verdi")
    fun utvidetAnsatt(ansattId: TIdent) =
        graph.utvidetAnsatt(ansattId)

    @Cacheable(GRAPH,key = "#root.methodName + ':' + #navIdent")
    fun grupperForAnsatt(navIdent: AnsattId, oid: UUID) =
        medOidOppfrisking(navIdent, oid) { graph.grupper(it) }

    fun antallMedlemmerIGrupper(vararg gruppeIder: UUID): Map<UUID, Long> =
        graph.antallMedlemmer(*gruppeIder)

    private fun <T> medOidOppfrisking(ansattId: AnsattId, ansattOid: UUID, oppslag: (UUID) -> T): T =
        runCatching {
            oppslag(ansattOid)
        }.getOrElse {
            if (it is NotFoundRestException) {
                oppslag(nyOid(ansattId))
            }
            else throw it
        }

    private fun nyOid(navIdent: AnsattId): UUID {
        cache.delete(OID_CACHE, navIdent.verdi).also {
            log.info("Slettet cache innslag før henting av ny oid $navIdent")
        }
        return oid.ansattOid(navIdent) ?: throw NotFoundRestException(currentUri, "Fant ikke ny oid for $navIdent i Entra, selv etter cache-opprydding og nytt oppslag")
    }

    private fun erInaktiv(ansattId: AnsattId) =
        cache.inneholder(INAKTIVE, ansattId.verdi).also {
            if (it) {
                log.info("$ansattId er inaktiv")
            }
        }

    override fun toString() =
        "${javaClass.simpleName} [graph=$graph]"
}

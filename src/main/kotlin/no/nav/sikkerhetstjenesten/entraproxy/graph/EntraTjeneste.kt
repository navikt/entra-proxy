package no.nav.sikkerhetstjenesten.entraproxy.graph

import no.nav.sikkerhetstjenesten.entraproxy.cache.CacheInaktiveNavIdenter.Companion.INAKTIVE
import no.nav.sikkerhetstjenesten.entraproxy.graph.EntraGraphClient.Companion.GRAPH
import no.nav.sikkerhetstjenesten.entraproxy.graph.MedlemmerConfig.Companion.MEDLEMMER
import no.nav.sikkerhetstjenesten.felles.cache.CacheOperations
import no.nav.sikkerhetstjenesten.felles.rest.NotFoundRestException
import no.nav.sikkerhetstjenesten.felles.rest.RestRetryingWhenRecoverableService
import org.slf4j.LoggerFactory.getLogger
import org.springframework.cache.annotation.Cacheable
import java.util.UUID

@RestRetryingWhenRecoverableService
class EntraTjeneste(private val graph: EntraGraphOperations, private val cache: CacheOperations, private val oid: OidOperations)  {

    private val log = getLogger(javaClass)

    @Cacheable(cacheNames = [GRAPH],  key = "#root.methodName + ':' + #ansattId.verdi")
    fun tema(ansattId: AnsattId) =
        if (erInaktiv(ansattId)) emptySet()
        else medOidOppfrisking(ansattId) { graph.temaer(it) }

    @Cacheable(cacheNames = [GRAPH],  key = "#root.methodName + ':' + #ansattId.verdi")
    fun enheter(ansattId: AnsattId) =
        if (erInaktiv(ansattId)) emptySet()
        else medOidOppfrisking(ansattId) { graph.enheter(it) }

    @Cacheable(MEDLEMMER, key = "#gruppeId.toString()")
    fun medlemmerIGruppe(gruppeNavn: String, gruppeId: UUID) =
        graph.gruppeMedlemmer("$gruppeId", gruppeNavn)

    @Cacheable(GRAPH,key = "#root.methodName + ':' + #ansattId.verdi")
    fun utvidetAnsatt(ansattId: AnsattId) =
        graph.utvidetAnsatt(ansattId)

    @Cacheable(GRAPH,key = "#root.methodName + ':' + #ansattId.verdi")
    fun utvidetAnsatt(ansattId: TIdent) =
        graph.utvidetAnsatt(ansattId)

    @Cacheable(GRAPH,key = "#root.methodName + ':' + #navIdent.verdi")
    fun grupperForAnsatt(navIdent: AnsattId) =
        medOidOppfrisking(navIdent) { graph.grupper(it) }

    fun antallMedlemmerIGrupper(vararg gruppeIder: UUID): Map<UUID, Long> =
        graph.antallMedlemmer(*gruppeIder)

    private fun <T> medOidOppfrisking(ansattId: AnsattId, oppslag: (UUID) -> Set<T>): Set<T> =
        runCatching {
            oppslag(oid.ansattOid(ansattId) ?: return emptySet())
        }.getOrElse {
            if (it is NotFoundRestException) {
                oppslag(oid.nyOid(ansattId))
            }
            else throw it
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

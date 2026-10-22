package no.nav.sikkerhetstjenesten.entraproxy.graph

import no.nav.sikkerhetstjenesten.entraproxy.graph.EntraGraphClient.Companion.GRAPH
import no.nav.sikkerhetstjenesten.entraproxy.graph.MedlemmerConfig.Companion.MEDLEMMER
import no.nav.sikkerhetstjenesten.felles.rest.RestRetryingWhenRecoverableService
import org.slf4j.LoggerFactory.getLogger
import org.springframework.cache.annotation.Cacheable
import java.util.UUID

@RestRetryingWhenRecoverableService
class EntraTjeneste(private val graph: EntraGraphOperations, private val inaktive: InaktiveAnsatte, private val oid: EntraOidOperations, private val enheter: EnhetOperations)  {

    private val log = getLogger(javaClass)

    @Cacheable(cacheNames = [GRAPH],  key = "#root.methodName + ':' + #ansattId.verdi")
    fun tema(ansattId: AnsattId) =
        if (erInaktiv(ansattId)) emptySet()
        else medOidOppfrisking(ansattId) { graph.temaer(it) }

    @Cacheable(cacheNames = [GRAPH],  key = "#root.methodName + ':' + #ansattId.verdi")
    fun enheter(ansattId: AnsattId): Set<Enhet> =
        if (erInaktiv(ansattId)) emptySet()
        else medOidOppfrisking(ansattId) { graph.enheter(it) }.mapTo(mutableSetOf()) { Enhet(it, enheter.navn(it)) }

    @Cacheable(MEDLEMMER, key = "#gruppeId.toString()")
    fun medlemmerIGruppe(gruppeId: UUID) =
        graph.gruppeMedlemmer("$gruppeId")

    @Cacheable(GRAPH,key = "#root.methodName + ':' + #ansattId.verdi")
    fun utvidetAnsatt(ansattId: AnsattId) =
        graph.utvidetAnsatt(ansattId)?.medEnhetsnavn()

    @Cacheable(GRAPH,key = "#root.methodName + ':' + #ansattId.verdi")
    fun utvidetAnsatt(ansattId: TIdent) =
        graph.utvidetAnsatt(ansattId)?.medEnhetsnavn()

    @Cacheable(GRAPH,key = "#root.methodName + ':' + #navIdent.verdi")
    fun grupper(navIdent: AnsattId) =
        medOidOppfrisking(navIdent) { graph.grupper(it) }

    fun antallMedlemmerIGrupper(vararg gruppeIder: UUID): Map<UUID, Long> =
        graph.antallMedlemmer(*gruppeIder)

    fun medlemmerIGrupperSomMatcher(gruppenavnSubstring: String): List<GruppeMedMedlemmer> =
        graph.grupperSomMatcher(gruppenavnSubstring).also {
            log.info("Fant ${it.size} grupper som matcher '$gruppenavnSubstring'")
        }
            .mapNotNull { gruppe ->
                log.info("Henter medlemmer for gruppe ${gruppe.rolle} med id ${gruppe.gruppeId}")
                gruppe.gruppeId?.let { gruppeId ->
                    GruppeMedMedlemmer(gruppeId, gruppe.rolle, graph.gruppeMedlemmer("$gruppeId"))
                }
            }.also {
                log.info("Hentet $it med medlemmer som matcher '$gruppenavnSubstring'")
            }

    fun grupperPerAnsattSomMatcher(gruppenavnSubstring: String): List<AnsattMedGrupper> =
        medlemmerIGrupperSomMatcher(gruppenavnSubstring).also {
            log.info("Fant ${it.size} grupper som matcher '$gruppenavnSubstring'")
        }
            .flatMap { gruppe -> gruppe.medlemmer.map { it to gruppe.gruppenavn } }
            .groupBy({ it.first }, { it.second })
            .map { (ansatt, grupper) -> AnsattMedGrupper(ansatt, grupper.toSortedSet()) }
            .sortedBy { it.ansatt }

    private fun <T> medOidOppfrisking(ansattId: AnsattId, oppslag: (UUID) -> Set<T>): Set<T> =
        runCatching {
            oppslag(oid.ansattOid(ansattId) ?: return emptySet())
        }.getOrElse {
            if (it is EntraIkkeFunnetException) {
                oppslag(oid.nyOid(ansattId))
            }
            else throw it
        }

    private fun UtvidetAnsatt.medEnhetsnavn() =
        copy(enhet = Enhet(enhet.enhetnummer, enheter.navn(enhet.enhetnummer)))

    private fun erInaktiv(ansattId: AnsattId) =
        inaktive.erInaktiv(ansattId).also {
            if (it) {
                log.info("$ansattId er inaktiv")
            }
        }

    override fun toString() =
        "${javaClass.simpleName} [graph=$graph]"
}

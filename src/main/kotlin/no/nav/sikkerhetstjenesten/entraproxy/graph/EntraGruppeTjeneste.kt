package no.nav.sikkerhetstjenesten.entraproxy.graph

import no.nav.sikkerhetstjenesten.entraproxy.felles.hentParallelt
import no.nav.sikkerhetstjenesten.entraproxy.graph.MedlemmerConfig.Companion.MEDLEMMER
import no.nav.sikkerhetstjenesten.felles.rest.RestRetryingWhenRecoverableService
import org.slf4j.LoggerFactory.getLogger
import org.springframework.cache.annotation.Cacheable
import java.util.UUID

@RestRetryingWhenRecoverableService
class EntraGruppeTjeneste(private val graph: EntraGraphOperations) {

    private val log = getLogger(javaClass)

    @Cacheable(MEDLEMMER, key = "#gruppeId.toString()")
    fun medlemmerIGruppe(gruppeId: UUID) =
        graph.gruppeMedlemmer("$gruppeId")

    fun antallMedlemmerIGrupper(vararg gruppeIder: UUID): Map<UUID, Long> =
        graph.antallMedlemmer(*gruppeIder)

    fun medlemmerIGrupperSomMatcher(substring: String): List<GruppeMedMedlemmer> =
        graph.grupperSomMatcher(substring).also {
            log.info("Fant {} grupper som matcher '{}'", it.size, substring)
            if (it.size > MAKS_MATCHENDE_GRUPPER) {
                throw ForMangeMatchendeGrupperException(substring, it.size, MAKS_MATCHENDE_GRUPPER)
            }
        }
            .mapNotNull {
                gruppe -> gruppe.gruppeId?.let {
                    it to gruppe.rolle
                }
            }
            .let { grupper ->
                hentParallelt(grupper, MAKS_SAMTIDIGE_KALL) { (gruppeId, rolle) ->
                    log.info("Henter parallelt medlemmer for gruppe {} med id {}", rolle, gruppeId)
                    GruppeMedMedlemmer(gruppeId, rolle, graph.gruppeMedlemmer("$gruppeId"))
                }
            }.also {
                log.info("Hentet {} grupper med medlemmer som matcher '{}'", it.size, substring)
            }

    fun grupperPerAnsattSomMatcher(substring: String) =
        medlemmerIGrupperSomMatcher(substring)
            .flatMap {
                gruppe -> gruppe.medlemmer.map {
                    it to gruppe.gruppenavn
                }
            }
            .groupBy({
                it.first
            }, {
                it.second
            })
            .map {
                (ansatt, grupper) -> AnsattMedGrupper(ansatt, grupper.toSortedSet())
            }
            .sortedBy {
                it.ansatt
            }

    override fun toString() =
        "${javaClass.simpleName} [graph=$graph]"

    companion object {
        private const val MAKS_SAMTIDIGE_KALL = 40
        private const val MAKS_MATCHENDE_GRUPPER = 100
    }
}

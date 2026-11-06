package no.nav.sikkerhetstjenesten.entraproxy.graph

import no.nav.sikkerhetstjenesten.entraproxy.graph.MedlemmerConfig.Companion.MEDLEMMER
import no.nav.sikkerhetstjenesten.felles.rest.RestRetryingWhenRecoverableService
import org.slf4j.LoggerFactory.getLogger
import org.springframework.cache.annotation.Cacheable
import java.util.UUID
import java.util.concurrent.Callable
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor
import java.util.concurrent.Semaphore

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
            log.info("Fant ${it.size} grupper som matcher '$substring'")
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
                hentParallelt(grupper) { (gruppeId, rolle) ->
                    log.info("Henter parallelt medlemmer for gruppe $rolle med id $gruppeId")
                    GruppeMedMedlemmer(gruppeId, rolle, graph.gruppeMedlemmer("$gruppeId"))
                }
            }.also {
                log.info("Hentet ${it.size} grupper med medlemmer som matcher '$substring'")
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

    private fun <T, R> hentParallelt(
        elementer: List<T>,
        maksSamtidige: Int = MAKS_SAMTIDIGE_KALL,
        oppslag: (T) -> R): List<R> {
        if (elementer.isEmpty()) return emptyList()
        val grense = Semaphore(maksSamtidige)
        return newVirtualThreadPerTaskExecutor().use { executor ->
            elementer
                .map { element ->
                    executor.submit(Callable {
                        grense.acquire()
                        try {
                            oppslag(element)
                        } finally {
                            grense.release()
                        }
                    })
                }
                .map { future ->
                    runCatching {
                        future.get()
                    }.getOrElse { throw (it as? ExecutionException)?.cause ?: it }
                }
        }
    }

    override fun toString() =
        "${javaClass.simpleName} [graph=$graph]"

    companion object {
        private const val MAKS_SAMTIDIGE_KALL = 40
        private const val MAKS_MATCHENDE_GRUPPER = 100
    }
}

package no.nav.sikkerhetstjenesten.entraproxy.graph

import no.nav.sikkerhetstjenesten.entraproxy.graph.EntraGraphClient.Companion.GRAPH
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
class EntraTjeneste(private val graph: EntraGraphOperations, private val inaktive: InaktivOperations, private val oid: EntraOidOperations, private val enheter: EnhetOperations)  {

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

    fun medlemmerIGrupperSomMatcher(subString: String): List<GruppeMedMedlemmer> =
        graph.grupperSomMatcher(subString).also {
            log.info("Fant ${it.size} grupper som matcher '$subString'")
            if (it.size > MAKS_MATCHENDE_GRUPPER) {
                throw ForMangeMatchendeGrupperException(subString, it.size, MAKS_MATCHENDE_GRUPPER)
            }
        }
            .mapNotNull { gruppe -> gruppe.gruppeId?.let { it to gruppe.rolle } }
            .let { grupper ->
                hentParallelt(grupper) { (gruppeId, rolle) ->
                    log.info("Henter parallelt medlemmer for gruppe $rolle med id $gruppeId")
                    GruppeMedMedlemmer(gruppeId, rolle, graph.gruppeMedlemmer("$gruppeId"))
                }
            }.also {
                log.info("Hentet ${it.size} grupper med medlemmer som matcher '$subString'")
            }

    fun grupperPerAnsattSomMatcher(gruppenavnSubstring: String): List<AnsattMedGrupper> =
        medlemmerIGrupperSomMatcher(gruppenavnSubstring)
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

    private fun <T, R> hentParallelt(elementer: List<T>, maksSamtidige: Int = MAKS_SAMTIDIGE_KALL, oppslag: (T) -> R): List<R> {
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
                    runCatching { future.get() }
                        .getOrElse { throw (it as? ExecutionException)?.cause ?: it }
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

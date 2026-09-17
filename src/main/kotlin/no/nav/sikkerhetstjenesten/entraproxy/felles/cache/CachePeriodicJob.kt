package no.nav.sikkerhetstjenesten.entraproxy.felles.cache

import no.nav.boot.conditionals.ConditionalOnGCP
import no.nav.sikkerhetstjenesten.entraproxy.graph.EntraTjeneste
import org.slf4j.LoggerFactory.getLogger
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.util.UUID
import java.util.concurrent.TimeUnit.MINUTES

@Component
@ConditionalOnGCP
class CachePeriodicJob(private val entra: EntraTjeneste, private val cacheOperations: CacheOperations, @Value("\${groups.disabled}") private val uuid: UUID) {

    private val log = getLogger(javaClass)

    @Scheduled(fixedRate = INTERVAL_MINUTES, timeUnit = MINUTES)
    fun kjørPeriodisk() {
        runCatching {
            val medlemmer = entra.medlemmerIGruppe(uuid).mapTo(mutableSetOf()) { it.navIdent.verdi
            }
            log.info("Periodisk cache-jobb OK, {} medlemmer i gruppe {} oppdatert", medlemmer.size, uuid)
            //cacheOperations.putSet("inaktive",emptySet())
        }.onSuccess {
            log.info("Periodisk cache-jobb OK, {} sett oppdatert", it)
        }.onFailure {
            log.warn("Periodisk cache-jobb feilet", it)
        }
    }

    companion object {
        private const val INTERVAL_MINUTES = 1L
    }
}

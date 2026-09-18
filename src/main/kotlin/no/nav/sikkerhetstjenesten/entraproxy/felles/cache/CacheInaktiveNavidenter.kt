package no.nav.sikkerhetstjenesten.entraproxy.felles.cache

import io.micrometer.core.annotation.Timed
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
class CacheInaktiveNavidenter(private val entra: EntraTjeneste, private val cache: CacheOperations, @Value($$"${groups.disabled}") private val uuid: UUID) {

    private val log = getLogger(javaClass)

    @Timed
    @Scheduled(fixedRate = INTERVAL_MINUTES, timeUnit = MINUTES)
    fun oppdaterCache() {
        runCatching {
            val medlemmer = entra.gruppeMedlemmer("$uuid").mapTo(mutableSetOf()) {
                it.navIdent.verdi
            }
            cache.replaceSet(INAKTIVE,medlemmer)
            log.info("Periodisk cache-jobb OK, la til ${medlemmer.size} inaktive medlemmer i cache")
        }.onFailure {
            log.warn("Periodisk cache-jobb feilet", it)
        }
    }

    companion object {
        const val INAKTIVE = "inaktive"
        private const val INTERVAL_MINUTES = 1L
    }
}

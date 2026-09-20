package no.nav.sikkerhetstjenesten.entraproxy.felles.cache

import io.micrometer.core.annotation.Timed
import no.nav.boot.conditionals.ConditionalOnGCP
import no.nav.sikkerhetstjenesten.entraproxy.felles.utils.LeaderAware
import no.nav.sikkerhetstjenesten.entraproxy.graph.EntraTjeneste
import org.slf4j.LoggerFactory.getLogger
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.util.UUID
import java.util.concurrent.TimeUnit.MINUTES
import kotlin.time.measureTimedValue

@Component
@ConditionalOnGCP
class CacheInaktiveNavIdenter(private val entra: EntraTjeneste, private val cache: CacheOperations, @Value($$"${groups.disabled}") private val uuid: UUID) : LeaderAware(true) {

    private val log = getLogger(javaClass)

    @Timed
    @Scheduled(fixedRate = INTERVAL_MINUTES, timeUnit = MINUTES, initialDelay = 1)
    fun oppdaterCache() =
        somLeder {
            val måling = measureTimedValue {
                runCatching {
                    cache.replaceSet(INAKTIVE, entra.gruppeMedlemmer("$uuid").mapTo(mutableSetOf()) {
                        it.navIdent.verdi
                    })
                    cache.getSet(INAKTIVE)
                }
            }
            måling.value.onSuccess {
                log.info(
                    "Periodisk cache-jobb OK, la til {} inaktive Nav-identer i cache på {}ms",
                    it.size,
                    måling.duration.inWholeMilliseconds
                )
            }.onFailure {
                log.warn("Periodisk cache-jobb feilet", it)
            }
        }

    companion object {
        const val INAKTIVE = "inaktive"
        private const val INTERVAL_MINUTES = 15L
    }
}

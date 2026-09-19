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
import kotlin.system.measureTimeMillis

@Component
@ConditionalOnGCP
class CacheInaktiveNavidenter(private val entra: EntraTjeneste, private val cache: CacheOperations, @Value($$"${groups.disabled}") private val uuid: UUID) : LeaderAware(true) {

    private val log = getLogger(javaClass)

    @Timed
    @Scheduled(fixedRate = INTERVAL_MINUTES, timeUnit = MINUTES)
    fun oppdaterCache() {
        somLeder {
            val varighet = measureTimeMillis {
                runCatching {
                    val navIdenter = entra.gruppeMedlemmer("$uuid").mapTo(mutableSetOf()) { it.navIdent.verdi }
                    if (navIdenter.isNotEmpty()) {
                        cache.replaceSet(STAGING, navIdenter)
                        cache.renameSet(STAGING, INAKTIVE)
                    } else {
                        cache.deleteSet(INAKTIVE)
                    }
                    log.info("Periodisk cache-jobb OK, la til {} inaktive medlemmer i cache", cache.getSet(INAKTIVE).size)
                }.onFailure {
                    log.warn("Periodisk cache-jobb feilet", it)
                }
            }
            log.info("Periodisk cache-jobb for inaktive navident tok {}ms", varighet)
        }
    }
    companion object {
        const val INAKTIVE = "inaktive"
        private const val STAGING = "$INAKTIVE:staging"
        private const val INTERVAL_MINUTES = 15L
    }
}

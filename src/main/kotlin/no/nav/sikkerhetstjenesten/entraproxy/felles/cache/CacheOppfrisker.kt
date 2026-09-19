package no.nav.sikkerhetstjenesten.entraproxy.felles.cache

import org.slf4j.LoggerFactory.getLogger
import kotlin.system.measureTimeMillis


abstract class AbstractCacheOppfrisker : CacheOppfrisker {
    protected val log = getLogger(javaClass)

    protected abstract fun doOppfrisk(nøkkelElementer: CacheNøkkel)

    final override fun oppfrisk(nøkkelElementer: CacheNøkkel) {
        val duration = measureTimeMillis {
            runCatching {
                doOppfrisk(nøkkelElementer)
                log.info("Oppfrisking av $nøkkelElementer OK")
            }.getOrElse {
                loggOppfriskingFeilet(nøkkelElementer, it)
            }
        }
        log.info("Oppfrisking tok ${duration}ms for $nøkkelElementer")
    }
    protected fun loggOppfriskingFeilet(elementer: CacheNøkkel, feil: Throwable) {
        log.warn("Oppfrisking av $elementer feilet", feil)
    }
}

interface CacheOppfrisker {
    val cacheName: String
    fun oppfrisk(nøkkelElementer: CacheNøkkel)
}
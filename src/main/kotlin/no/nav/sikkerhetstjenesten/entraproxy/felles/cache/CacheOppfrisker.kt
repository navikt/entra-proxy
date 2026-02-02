package no.nav.sikkerhetstjenesten.entraproxy.felles.cache

import io.opentelemetry.instrumentation.annotations.WithSpan
import org.slf4j.LoggerFactory.getLogger
import kotlin.system.measureTimeMillis


abstract class AbstractCacheOppfrisker : CacheOppfrisker {
    protected val log = getLogger(javaClass)

    protected abstract fun doOppfrisk(nøkkelElementer: CacheNøkkelElementer)

    @WithSpan
    final override fun oppfrisk(nøkkelElementer: CacheNøkkelElementer) {
        val duration = measureTimeMillis {
            runCatching {
                doOppfrisk(nøkkelElementer)
                log.info("Oppfrisking av ${nøkkelElementer.cacheName}::${nøkkelElementer.id} OK")
            }.getOrElse {
                loggOppfriskingFeilet(`nøkkelElementer`, it)
            }
        }
        log.info("Oppfrisking tok ${duration}ms for ${nøkkelElementer.cacheName}::${nøkkelElementer.id}")
    }
    protected fun loggOppfriskingFeilet(elementer: CacheNøkkelElementer, feil: Throwable) {
        log.warn("Oppfrisking av ${elementer.cacheName}::${elementer.id} feilet", feil)
    }
}

interface CacheOppfrisker {
    val cacheName: String
    fun oppfrisk(nøkkelElementer: CacheNøkkelElementer)
}
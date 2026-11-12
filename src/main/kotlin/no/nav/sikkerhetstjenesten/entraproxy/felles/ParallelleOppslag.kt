package no.nav.sikkerhetstjenesten.entraproxy.felles

import io.opentelemetry.context.Context
import org.slf4j.LoggerFactory.getLogger
import java.lang.System.nanoTime
import java.util.concurrent.Callable
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit.NANOSECONDS

private val log = getLogger("no.nav.sikkerhetstjenesten.entraproxy.felles.ParallelleOppslag")
private const val LOGG_VENTETID_FRA_MS = 100L

internal fun <T, R> hentParallelt(elementer: List<T>, maksSamtidige: Int, oppslag: (T) -> R): List<R> {
    require(maksSamtidige > 0) { "maksSamtidige må være større enn null" }
    if (elementer.isEmpty()) return emptyList()
    val grense = Semaphore(maksSamtidige)
    val context = Context.current()
    return newVirtualThreadPerTaskExecutor().use { executor ->
        elementer
            .map { element ->
                executor.submit(context.wrap(Callable {
                    val start = nanoTime()
                    grense.acquire()
                    try {
                        val ventetidMs = NANOSECONDS.toMillis(nanoTime() - start)
                        if (ventetidMs >= LOGG_VENTETID_FRA_MS) {
                            log.info("Ventet {} ms på semafor (maks {} samtidige oppslag)", ventetidMs, maksSamtidige)
                        }
                        oppslag(element)
                    } finally {
                        grense.release()
                    }
                }))
            }
            .map { future ->
                try {
                    future.get()
                } catch (feil: ExecutionException) {
                    throw feil.cause ?: feil
                }
            }
    }
}

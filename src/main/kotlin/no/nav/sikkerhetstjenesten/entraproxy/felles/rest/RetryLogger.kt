package no.nav.sikkerhetstjenesten.entraproxy.felles.rest

import org.slf4j.LoggerFactory.getLogger
import org.springframework.context.event.EventListener
import org.springframework.resilience.retry.MethodRetryEvent
import org.springframework.stereotype.Component
import kotlin.jvm.javaClass

@Component
class RetryLogger {
    private val log = getLogger(javaClass)

    @EventListener(MethodRetryEvent::class)
    fun onEvent(event: MethodRetryEvent) {
        if (event.isRetryAborted) {
            log.warn("Aborting method ${event.method.name}, retry exhausted",event.failure)
        }
        else  {
            log.info("Retrying method '${event.method.name}' time due to exception: ${event.failure}")
        }
    }
}
package no.nav.sikkerhetstjenesten.entraproxy.felles.rest


import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Tags
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.MDC
import org.springframework.stereotype.Component
import org.springframework.web.servlet.HandlerInterceptor

@Component
class ConsumerAwareHandlerInterceptor(private val token: Token, private val registry: MeterRegistry) : HandlerInterceptor {

    override fun preHandle(request: HttpServletRequest, response: HttpServletResponse, handler: Any): Boolean {
        MDC.put(CONSUMER_ID, token.systemAndNs)
        registry.counter(METRIC, Tags.of("remote_system",token.systemNavn)).increment()
        return true
    }
    companion object  {
        private const val METRIC = "http_requests_by_remote_system"
        const val CONSUMER_ID = "consumerId"
        const val USER_ID = "userId"
    }
}
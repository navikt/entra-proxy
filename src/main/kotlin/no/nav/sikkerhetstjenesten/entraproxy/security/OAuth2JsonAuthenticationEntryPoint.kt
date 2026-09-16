package no.nav.sikkerhetstjenesten.entraproxy.security

import io.opentelemetry.api.trace.Span
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory.getLogger
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatus.UNAUTHORIZED
import org.springframework.http.MediaType
import org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON_VALUE
import org.springframework.http.ProblemDetail.forStatusAndDetail
import org.springframework.security.core.AuthenticationException
import org.springframework.security.web.AuthenticationEntryPoint
import org.springframework.stereotype.Component
import tools.jackson.databind.json.JsonMapper
import java.net.URI

@Component
class OAuth2JsonAuthenticationEntryPoint(private val mapper: JsonMapper) : AuthenticationEntryPoint {
    private val log = getLogger(javaClass)

    override fun commence(req: HttpServletRequest, res: HttpServletResponse, e: AuthenticationException) {
        log.warn("Autentisering feilet for uri={}, message={}", req.requestURI, e.message, e)
        with(res) {
            status = UNAUTHORIZED.value()
            contentType = APPLICATION_PROBLEM_JSON_VALUE
            mapper.writeValue(writer, securityProblemDetail(UNAUTHORIZED, MANGLER_BEARER_TOKEN))
        }
    }

    private fun securityProblemDetail(status: HttpStatus, detail: String) =
        forStatusAndDetail(status, detail).apply {
            type = TYPE_URI
            title = "${status.value()}"
            properties = mapOf("traceId" to Span.current().spanContext.traceId)
        }

    companion object {
        private const val MANGLER_BEARER_TOKEN = "Bruker er ikke logget inn. Mangler Bearer token i Authorization header."
        private val TYPE_URI = URI.create("https://confluence.adeo.no/spaces/TM/pages/758383588/entra-proxy")
    }
}
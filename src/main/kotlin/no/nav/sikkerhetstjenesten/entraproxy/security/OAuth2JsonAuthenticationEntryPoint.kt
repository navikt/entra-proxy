package no.nav.sikkerhetstjenesten.entraproxy.security

import io.opentelemetry.api.trace.Span
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory.getLogger
import org.springframework.http.HttpStatus.UNAUTHORIZED
import org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON_VALUE
import org.springframework.http.ProblemDetail.forStatusAndDetail
import org.springframework.security.core.AuthenticationException
import org.springframework.security.web.AuthenticationEntryPoint
import org.springframework.stereotype.Component
import tools.jackson.databind.json.JsonMapper
import java.net.URI.create

@Component
class OAuth2JsonAuthenticationEntryPoint(private val mapper: JsonMapper) : AuthenticationEntryPoint {
    private val log = getLogger(javaClass)

    override fun commence(req: HttpServletRequest, res: HttpServletResponse, e: AuthenticationException) {
        log.warn("Autentisering feilet for uri={}, message={}", req.requestURI, e.message, e)
        with(res) {
            status = UNAUTHORIZED.value()
            contentType = APPLICATION_PROBLEM_JSON_VALUE
            mapper.writeValue(writer, securityProblemDetail())
        }
    }

    private fun securityProblemDetail() =
        forStatusAndDetail(UNAUTHORIZED, "Bruker er ikke logget inn. Mangler Bearer token i Authorization header").apply {
            type = create("https://confluence.adeo.no/spaces/TM/pages/758383588/entra-proxy")
            title = "${UNAUTHORIZED.value()}"
            properties = mapOf("traceId" to Span.current().spanContext.traceId)
        }

}
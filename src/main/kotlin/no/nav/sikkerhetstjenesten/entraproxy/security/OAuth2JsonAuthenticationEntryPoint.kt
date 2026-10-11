package no.nav.sikkerhetstjenesten.entraproxy.security

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import no.nav.sikkerhetstjenesten.felles.security.securityProblemDetail
import org.slf4j.LoggerFactory.getLogger
import org.springframework.http.HttpStatus.UNAUTHORIZED
import org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON_VALUE
import org.springframework.security.core.AuthenticationException
import org.springframework.security.web.AuthenticationEntryPoint
import org.springframework.stereotype.Component
import tools.jackson.databind.json.JsonMapper

@Component
class OAuth2JsonAuthenticationEntryPoint(private val mapper: JsonMapper) : AuthenticationEntryPoint {
    private val log = getLogger(javaClass)

    override fun commence(req: HttpServletRequest, res: HttpServletResponse, e: AuthenticationException) {
        log.warn("Autentisering feilet for uri={}, message={}", req.requestURI, e.message, e)
        with(res) {
            status = UNAUTHORIZED.value()
            contentType = APPLICATION_PROBLEM_JSON_VALUE
            mapper.writeValue(writer, securityProblemDetail(UNAUTHORIZED, "Bruker er ikke logget inn. Mangler Bearer token i Authorization header", TYPE_URI))
        }
    }
}
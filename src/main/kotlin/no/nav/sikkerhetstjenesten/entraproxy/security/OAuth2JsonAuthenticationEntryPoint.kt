package no.nav.sikkerhetstjenesten.entraproxy.security

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.core.AuthenticationException
import org.springframework.security.web.AuthenticationEntryPoint
import org.springframework.stereotype.Component
import tools.jackson.databind.json.JsonMapper

@Component
class OAuth2JsonAuthenticationEntryPoint(private val mapper: JsonMapper) : AuthenticationEntryPoint {
    override fun commence(req: HttpServletRequest, res: HttpServletResponse, e: AuthenticationException) =
        with(res) {
            status = HttpStatus.UNAUTHORIZED.value()
            contentType = MediaType.APPLICATION_PROBLEM_JSON_VALUE
            mapper.writeValue(writer, securityProblemDetail(HttpStatus.UNAUTHORIZED, MANGLER_BEARER_TOKEN))
        }
}
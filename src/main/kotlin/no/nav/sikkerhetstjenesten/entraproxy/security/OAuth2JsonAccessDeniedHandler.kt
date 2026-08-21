package no.nav.sikkerhetstjenesten.entraproxy.security

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.web.access.AccessDeniedHandler
import org.springframework.stereotype.Component
import tools.jackson.databind.json.JsonMapper

@Component
class OAuth2JsonAccessDeniedHandler(private val mapper: JsonMapper) : AccessDeniedHandler {
    override fun handle(req: HttpServletRequest, res: HttpServletResponse, e: AccessDeniedException) =
        with(res) {
            status = HttpStatus.FORBIDDEN.value()
            contentType = MediaType.APPLICATION_PROBLEM_JSON_VALUE
            mapper.writeValue(writer, securityProblemDetail(HttpStatus.FORBIDDEN, e.message ?: "Access denied"))
        }
}
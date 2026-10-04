package no.nav.sikkerhetstjenesten.entraproxy.security

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import no.nav.sikkerhetstjenesten.felles.security.securityProblemDetail
import org.springframework.http.HttpStatus.FORBIDDEN
import org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON_VALUE
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.web.access.AccessDeniedHandler
import org.springframework.stereotype.Component
import tools.jackson.databind.json.JsonMapper

@Component
class OAuth2JsonAccessDeniedHandler(private val mapper: JsonMapper) :
    AccessDeniedHandler {


    override fun handle(req: HttpServletRequest, res: HttpServletResponse, e: AccessDeniedException) {
        with(res) {
            status = FORBIDDEN.value()
            contentType = APPLICATION_PROBLEM_JSON_VALUE
            mapper.writeValue(writer, securityProblemDetail(FORBIDDEN, e.message ?: "Access Denied",TYPE_URI))
        }
    }
}
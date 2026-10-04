package no.nav.sikkerhetstjenesten.entraproxy.security

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import no.nav.sikkerhetstjenesten.felles.security.AbstractOAuth2JsonAccessDeniedHandler
import no.nav.sikkerhetstjenesten.felles.security.AuthContext
import no.nav.sikkerhetstjenesten.felles.security.securityProblemDetail
import org.springframework.http.HttpStatus.FORBIDDEN
import org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON_VALUE
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.web.access.AccessDeniedHandler
import org.springframework.stereotype.Component
import tools.jackson.databind.json.JsonMapper

@Component
class OAuth2JsonAccessDeniedHandler( mapper: JsonMapper, ctx: AuthContext) :
    AbstractOAuth2JsonAccessDeniedHandler(mapper,ctx,TYPE_URI) {
    override fun preHandle(req: HttpServletRequest,
                           res: HttpServletResponse) = Unit
    }
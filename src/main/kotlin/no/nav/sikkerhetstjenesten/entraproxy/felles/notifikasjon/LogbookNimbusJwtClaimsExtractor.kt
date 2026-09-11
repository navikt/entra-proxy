package no.nav.sikkerhetstjenesten.entraproxy.felles.notifikasjon

import com.nimbusds.jwt.SignedJWT
import no.nav.sikkerhetstjenesten.entraproxy.felles.withTimestampsInCurrentTimezone
import org.springframework.http.HttpHeaders
import org.springframework.security.oauth2.core.OAuth2AccessToken.TokenType
import org.springframework.stereotype.Component
import org.zalando.logbook.HttpRequest
import org.zalando.logbook.attributes.AttributeExtractor
import org.zalando.logbook.attributes.HttpAttributes

@Component
class LogbookNimbusJwtClaimsExtractor : AttributeExtractor {

    override fun extract(request: HttpRequest): HttpAttributes {
        val auth = request.headers.getFirst(HttpHeaders.AUTHORIZATION) ?: return HttpAttributes.EMPTY
        return HttpAttributes(SignedJWT.parse(auth.removePrefix(TokenType.BEARER.value) + " ").jwtClaimsSet.claims.withTimestampsInCurrentTimezone())
    }
}
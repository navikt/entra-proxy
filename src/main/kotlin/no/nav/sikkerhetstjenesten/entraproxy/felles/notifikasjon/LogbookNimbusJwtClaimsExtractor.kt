package no.nav.sikkerhetstjenesten.entraproxy.felles.notifikasjon

import com.nimbusds.jwt.SignedJWT
import no.nav.sikkerhetstjenesten.entraproxy.felles.utils.extensions.TimeExtensions.OSLO
import org.springframework.context.annotation.Primary
import org.springframework.http.HttpHeaders.AUTHORIZATION
import org.springframework.security.oauth2.core.OAuth2AccessToken.TokenType.BEARER
import org.springframework.stereotype.Component
import org.zalando.logbook.HttpRequest
import org.zalando.logbook.attributes.AttributeExtractor
import org.zalando.logbook.attributes.HttpAttributes
import org.zalando.logbook.attributes.HttpAttributes.EMPTY
import java.util.Date

@Component
@Primary
class LogbookNimbusJwtClaimsExtractor : AttributeExtractor {

    override fun extract(request: HttpRequest): HttpAttributes {
        val auth = request.headers.getFirst(AUTHORIZATION) ?: return EMPTY
        return HttpAttributes(SignedJWT.parse(auth.removePrefix(BEARER.value) + " ").jwtClaimsSet.claims.withTimestampsInCurrentTimezone())
    }
}
private fun Map<String, Any>.withTimestampsInCurrentTimezone() =
    mapValues {
            (_, value) -> (value as? Date)?.toInstant()?.atZone(OSLO) ?: value
    }
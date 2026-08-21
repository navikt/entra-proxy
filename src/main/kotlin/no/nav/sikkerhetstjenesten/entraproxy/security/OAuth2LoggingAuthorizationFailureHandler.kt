package no.nav.sikkerhetstjenesten.entraproxy.security

import no.nav.sikkerhetstjenesten.entraproxy.felles.utils.extensions.DomainExtensions
import no.nav.sikkerhetstjenesten.entraproxy.security.OAuth2DownstreamURICapturingInterceptor.OAuth2DownstreamURIContext
import org.slf4j.LoggerFactory
import org.springframework.security.core.Authentication
import org.springframework.security.oauth2.client.ClientAuthorizationException
import org.springframework.security.oauth2.client.OAuth2AuthorizationFailureHandler
import org.springframework.security.oauth2.core.OAuth2AuthorizationException

class OAuth2LoggingAuthorizationFailureHandler(
    private val delegate: OAuth2AuthorizationFailureHandler) : OAuth2AuthorizationFailureHandler {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun onAuthorizationFailure(e: OAuth2AuthorizationException, principal: Authentication, attr: Map<String, Any>) {
        val registrationId = (e as? ClientAuthorizationException)?.clientRegistrationId ?: DomainExtensions.UTILGJENGELIG
        val uri = OAuth2DownstreamURIContext.currentUri() ?: DomainExtensions.UTILGJENGELIG
        log.debug("OAuth2 authorization feilet for id=$registrationId, errorCode=${e.error.errorCode}, uri=$uri")
        delegate.onAuthorizationFailure(e, principal, attr)
    }
}
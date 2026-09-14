package no.nav.sikkerhetstjenesten.entraproxy.security

import no.nav.sikkerhetstjenesten.entraproxy.felles.OAuth2DownstreamURIContext
import no.nav.sikkerhetstjenesten.entraproxy.felles.OAuth2DownstreamURIContext.currentUri
import no.nav.sikkerhetstjenesten.entraproxy.felles.utils.extensions.TimeExtensions.OSLO
import org.slf4j.LoggerFactory.getLogger
import org.springframework.security.core.Authentication
import org.springframework.security.oauth2.client.OAuth2AuthorizationSuccessHandler
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService


class OAuth2LoggingAuthorizationSuccessHandler(
    private val service: OAuth2AuthorizedClientService,
    private val delegate: OAuth2AuthorizationSuccessHandler) : OAuth2AuthorizationSuccessHandler {

    private val log = getLogger(javaClass)

    override fun onAuthorizationSuccess(authorizedClient: OAuth2AuthorizedClient, principal: Authentication, attributes: Map<String, Any>) {
        val registrationId = authorizedClient.clientRegistration.registrationId
        val previousClient = service.loadAuthorizedClient<OAuth2AuthorizedClient>(registrationId, principal.name)
        val previousExpiry = previousClient?.expiry()
        val currentExpiry = authorizedClient.expiry()

        if (previousClient == null) {
            log.info("OAuth2 første autorisering: id=$registrationId, expiresAt=$currentExpiry, uri=$currentUri")
        } else {
            log.info("OAuth2 token fornyelse: id=$registrationId, oldExpiresAt=$previousExpiry, newExpiresAt=$currentExpiry, uri=$currentUri")
        }
        delegate.onAuthorizationSuccess(authorizedClient, principal, attributes)
    }

    private fun OAuth2AuthorizedClient.expiry() =
        accessToken.expiresAt?.atZone(OSLO)
}
package no.nav.sikkerhetstjenesten.entraproxy.security

import no.nav.sikkerhetstjenesten.entraproxy.felles.utils.extensions.DomainExtensions
import org.slf4j.LoggerFactory
import org.springframework.http.HttpRequest
import org.springframework.http.client.ClientHttpRequestExecution
import org.springframework.http.client.ClientHttpRequestInterceptor
import org.springframework.security.core.Authentication
import org.springframework.security.oauth2.client.ClientAuthorizationException
import org.springframework.security.oauth2.client.OAuth2AuthorizationFailureHandler
import org.springframework.security.oauth2.core.OAuth2AuthorizationException

class OAuth2DownstreamURICapturingInterceptor : ClientHttpRequestInterceptor {
    override fun intercept(request: HttpRequest, body: ByteArray, execution: ClientHttpRequestExecution) =
        try {
            OAuth2DownstreamURIContext.set(request.uri.toString())
            execution.execute(request, body)
        } finally {
            OAuth2DownstreamURIContext.clear()
        }

    internal object OAuth2DownstreamURIContext {
        private val downstreamUri = ThreadLocal<String?>()

        fun currentUri(): String? = downstreamUri.get()

        fun set(uri: String) {
            downstreamUri.set(uri)
        }

        fun clear() {
            downstreamUri.remove()
        }
    }

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
}
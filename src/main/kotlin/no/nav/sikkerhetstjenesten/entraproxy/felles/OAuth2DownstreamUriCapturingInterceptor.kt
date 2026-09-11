package no.nav.sikkerhetstjenesten.entraproxy.felles

import org.springframework.http.HttpRequest
import org.springframework.http.client.ClientHttpRequestExecution
import org.springframework.http.client.ClientHttpRequestInterceptor

class OAuth2DownstreamUriCapturingInterceptor : ClientHttpRequestInterceptor {
    override fun intercept(request: HttpRequest, body: ByteArray, execution: ClientHttpRequestExecution) =
        try {
            OAuth2DownstreamURIContext.set(request.uri.toString())
            execution.execute(request, body)
        } finally {
            OAuth2DownstreamURIContext.clear()
        }
}
package no.nav.sikkerhetstjenesten.entraproxy.felles

import org.springframework.http.HttpRequest
import org.springframework.http.client.ClientHttpRequestExecution
import org.springframework.http.client.ClientHttpRequestInterceptor
import org.springframework.http.client.ClientHttpResponse

class OAuth2DownstreamUriCapturingInterceptor : ClientHttpRequestInterceptor {
    override fun intercept(request: HttpRequest, body: ByteArray, execution: ClientHttpRequestExecution): ClientHttpResponse {
        OAuth2DownstreamURIContext.set(request.uri.toString())
        return execution.execute(request, body)
    }
}
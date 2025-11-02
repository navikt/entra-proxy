package no.nav.sikkerhetstjenesten.entraproxy.felles.rest

import org.springframework.http.HttpRequest
import org.springframework.http.client.ClientHttpRequestExecution
import org.springframework.http.client.ClientHttpRequestInterceptor
import org.springframework.http.client.ClientHttpResponse
import org.springframework.stereotype.Component

@Component
class TokenTypeRequestInterceptor(private val teller: TokenTypeTeller) : ClientHttpRequestInterceptor {
    override fun intercept(request: HttpRequest, body: ByteArray, execution: ClientHttpRequestExecution): ClientHttpResponse {
        if (request.uri.path.contains("monitoring")) return execution.execute(request, body)
        teller.tell()
        return execution.execute(request, body)
    }
}


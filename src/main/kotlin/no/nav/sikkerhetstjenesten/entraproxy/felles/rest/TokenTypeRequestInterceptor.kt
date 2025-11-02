package no.nav.sikkerhetstjenesten.entraproxy.felles.rest

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Tags
import org.slf4j.LoggerFactory.getLogger
import org.springframework.http.HttpHeaders.AUTHORIZATION
import org.springframework.http.HttpRequest
import org.springframework.http.client.ClientHttpRequestExecution
import org.springframework.http.client.ClientHttpRequestInterceptor
import org.springframework.http.client.ClientHttpResponse
import org.springframework.stereotype.Component

@Component
class LoggingRequestInterceptor(private val teller: TokenTypeTeller) : ClientHttpRequestInterceptor {
    private val log = getLogger(javaClass)
    override fun intercept(request: HttpRequest, body: ByteArray, execution: ClientHttpRequestExecution): ClientHttpResponse {
        if (request.uri.path.contains("monitoring")) {
            return execution.execute(request, body)
        }
        teller.tell()
       //log.trace("Headers for {}: {}", request.uri, request.headers/*.filter { !it.key.contains(AUTHORIZATION) }*/)
        if (!body.isEmpty()) {
          // log.debug("Body for {} {} : {} ",request.method, request.uri,String(body))
        }
        val response = execution.execute(request, body)
       if (!response.statusCode.is2xxSuccessful) {
           log.warn("Response status for {} {}: {}", request.method, request.uri, response.statusCode)
        }
        return response
    }
}

@Component
class TokenTypeTeller(registry: MeterRegistry, token: Token) :
    AbstractTeller(registry, token, "token.type", "Token type")

abstract class AbstractTeller(private val registry: MeterRegistry, private val token: Token, private val navn: String, private val beskrivelse: String) {

    open fun tell(tags: Tags = Tags.empty(), n:Int=1) =
        Counter.builder(navn)
            .description(beskrivelse)
            .tags(tags
                .and("token", TokenType.from(token).name.lowercase())
                .and("system", token.system)
                .and("clustersystem", token.clusterAndSystem))
            .register(registry)
            .increment(n.toDouble())
}
package no.nav.sikkerhetstjenesten.entraproxy.norg

import io.micrometer.context.ContextRegistry
import io.micrometer.context.ThreadLocalAccessor
import no.nav.sikkerhetstjenesten.entraproxy.norg.NorgProxyClient.Companion.NORG
import no.nav.sikkerhetstjenesten.felles.rest.DownstreamURIContext
import no.nav.sikkerhetstjenesten.felles.rest.IrrecoverableRestException
import no.nav.sikkerhetstjenesten.felles.rest.NotFoundRestException
import no.nav.sikkerhetstjenesten.felles.rest.RecoverableRestException
import org.springframework.boot.http.client.autoconfigure.reactive.ClientHttpConnectorBuilderCustomizer
import org.springframework.boot.http.client.reactive.ReactorClientHttpConnectorBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatus.NOT_FOUND
import org.springframework.http.HttpStatus.REQUEST_TIMEOUT
import org.springframework.http.HttpStatus.TOO_MANY_REQUESTS
import org.springframework.http.HttpStatusCode
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager
import org.springframework.security.oauth2.client.web.reactive.function.client.support.OAuth2WebClientHttpServiceGroupConfigurer
import org.springframework.web.reactive.function.client.ClientResponse
import org.springframework.web.reactive.function.client.ExchangeFilterFunction
import org.springframework.web.reactive.function.client.support.WebClientHttpServiceGroupConfigurer
import org.zalando.logbook.Logbook
import org.zalando.logbook.netty.LogbookClientHandler
import reactor.core.publisher.Hooks
import reactor.core.publisher.Mono

@Configuration
class NorgWebClientBeanConfig {

    init {
        Hooks.enableAutomaticContextPropagation()
        ContextRegistry.getInstance().registerThreadLocalAccessor(DownstreamUriThreadLocalAccessor)
    }

    @Bean
    fun norgLogbookConnectorCustomizer(logbook: Logbook) =
        ClientHttpConnectorBuilderCustomizer<ReactorClientHttpConnectorBuilder> { builder ->
            builder.withHttpClientCustomizer { httpClient ->
                httpClient.doOnConnected { connection ->
                    connection.addHandlerLast(LogbookClientHandler(logbook))
                }
            }
        }

    @Bean
    fun norgWebClientGroupConfigurer(manager: OAuth2AuthorizedClientManager) =
        WebClientHttpServiceGroupConfigurer { groups ->
            OAuth2WebClientHttpServiceGroupConfigurer.from(manager).configureGroups(groups)
            groups.filterByName(NORG).forEachClient { _, builder ->
                builder.filters { it.add(0, downstreamUriCapturingFilter()) }
                builder.defaultStatusHandler(HttpStatusCode::isError) { response ->
                    response.releaseBody().then(Mono.error(response.tilRestException()))
                }
            }
        }

    private fun downstreamUriCapturingFilter() =
        ExchangeFilterFunction { request, next ->
            next.exchange(request)
                .contextWrite { it.put(DownstreamUriThreadLocalAccessor.KEY, request.url().toString()) }
        }

    private fun ClientResponse.tilRestException(): Throwable {
        val status = statusCode()
        val uri = request().uri
        val identifikator = request().headers.getFirst(X_IDENTIFIKATOR)
        val statusTekst = (status as? HttpStatus)?.reasonPhrase ?: status.toString()
        return when (status.value()) {
            NOT_FOUND.value() -> NotFoundRestException(uri, identifikator)
            REQUEST_TIMEOUT.value(), TOO_MANY_REQUESTS.value() -> RecoverableRestException(status, uri, statusTekst)
            else -> if (status.is4xxClientError) IrrecoverableRestException(status, uri, statusTekst)
            else RecoverableRestException(status, uri, statusTekst)
        }
    }

    companion object {
        private const val X_IDENTIFIKATOR = "X-Identifikator"
    }
}

private object DownstreamUriThreadLocalAccessor : ThreadLocalAccessor<String> {

    const val KEY = "nav.downstream.uri"

    override fun key(): String = KEY

    override fun getValue(): String? =
        runCatching { DownstreamURIContext.currentUri.toString() }.getOrNull()

    override fun setValue(value: String) = DownstreamURIContext.set(value)

    override fun setValue() = DownstreamURIContext.clear()
}

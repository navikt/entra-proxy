package no.nav.sikkerhetstjenesten.entraproxy.nom

import no.nav.sikkerhetstjenesten.entraproxy.nom.NomGraphQLConfig.Companion.NOMGRAPH
import no.nav.sikkerhetstjenesten.felles.NoCoverageAnalysis
import no.nav.sikkerhetstjenesten.felles.graphql.GraphQLLoggingInterceptor
import no.nav.sikkerhetstjenesten.felles.rest.DownstreamUriCapturingInterceptor
import no.nav.sikkerhetstjenesten.felles.rest.PingableHealthIndicator
import org.springframework.beans.factory.ObjectProvider
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder.httpComponents
import org.springframework.boot.http.client.HttpClientSettings.defaults
import org.springframework.boot.http.client.HttpComponentsClientHttpRequestFactoryBuilder
import org.springframework.boot.http.client.autoconfigure.ClientHttpRequestFactoryBuilderCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.graphql.client.GraphQlClient
import org.springframework.graphql.client.HttpSyncGraphQlClient.builder
import org.springframework.security.oauth2.client.OAuth2AuthorizationFailureHandler
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager
import org.springframework.security.oauth2.client.web.client.OAuth2ClientHttpRequestInterceptor
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClient.Builder
import org.zalando.logbook.spring.LogbookClientHttpRequestInterceptor
import java.time.Duration

@Configuration
@NoCoverageAnalysis
class NomBeanConfig {


    @Bean
    @Qualifier(NOMGRAPH)
    fun nomGraphRestClient(builder: Builder,
                           cfg: NomGraphQLConfig,
                           factoryBuilderCustomizer: ClientHttpRequestFactoryBuilderCustomizer<HttpComponentsClientHttpRequestFactoryBuilder>,
                           @Value($$"${spring.http.serviceclient.clients.connect-timeout:3s}") connectTimeout: Duration,
                           logbookInterceptor: ObjectProvider<LogbookClientHttpRequestInterceptor>,
                           mgr: OAuth2AuthorizedClientManager, failureHandler: OAuth2AuthorizationFailureHandler) =
        builder
            .requestFactory(
                factoryBuilderCustomizer
                    .customize(httpComponents())
                    .build(defaults()
                        .withConnectTimeout(connectTimeout)
                        .withReadTimeout(cfg.lesetimeout)))
            .requestInterceptors {
                logbookInterceptor.ifAvailable {
                        interceptor -> it.add(interceptor)
                }
                it.add(DownstreamUriCapturingInterceptor())
                it.add(OAuth2ClientHttpRequestInterceptor(mgr).apply {
                    setClientRegistrationIdResolver { NOMGRAPH }
                    setAuthorizationFailureHandler(failureHandler)
                })
            }
            .build()

    @Bean
    @Qualifier(NOMGRAPH)
    fun nomGraphSyncGraphQLClient(@Qualifier(NOMGRAPH) client: RestClient, cfg: NomGraphQLConfig) =
        builder(client)
            .url(cfg.baseUri)
            .interceptors {
                it.addFirst(GraphQLLoggingInterceptor())
            }.build()

    @Bean
    fun nomHealthIndicator(cfg: NomGraphQLConfig, @Qualifier(NOMGRAPH) client: GraphQlClient) =
        PingableHealthIndicator(cfg) {
            client.document(PING_QUERY).executeSync()
        }

    private companion object {
        private const val PING_QUERY = "{ __typename }"
    }
}


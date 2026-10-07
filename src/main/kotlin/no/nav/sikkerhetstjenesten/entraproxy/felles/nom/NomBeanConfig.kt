package no.nav.sikkerhetstjenesten.entraproxy.felles.nom

import no.nav.sikkerhetstjenesten.entraproxy.felles.nom.NomGraphQLConfig.Companion.NOMGRAPH
import no.nav.sikkerhetstjenesten.felles.NoCoverageAnalysis
import no.nav.sikkerhetstjenesten.felles.graphql.GraphQLLoggingInterceptor
import no.nav.sikkerhetstjenesten.felles.rest.DownstreamUriCapturingInterceptor
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.graphql.client.HttpSyncGraphQlClient.builder
import org.springframework.security.oauth2.client.OAuth2AuthorizationFailureHandler
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager
import org.springframework.security.oauth2.client.web.client.OAuth2ClientHttpRequestInterceptor
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClient.Builder

@Configuration
@NoCoverageAnalysis
class NomBeanConfig {


    @Bean
    @Qualifier(NOMGRAPH)
    fun nomGraphRestClient(builder: Builder, mgr: OAuth2AuthorizedClientManager, failureHandler: OAuth2AuthorizationFailureHandler) =
        builder
            .requestInterceptors {
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
}


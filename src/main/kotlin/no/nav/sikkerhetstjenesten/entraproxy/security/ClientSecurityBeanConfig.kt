package no.nav.sikkerhetstjenesten.entraproxy.security

import no.nav.sikkerhetstjenesten.entraproxy.felles.FellesHttpClientInterceptors
import no.nav.sikkerhetstjenesten.entraproxy.felles.RestClientBeanConfig.Companion.headerAddingRequestInterceptor
import no.nav.sikkerhetstjenesten.entraproxy.graph.EntraGraphClient.Companion.GRAPH
import org.springframework.beans.factory.ObjectProvider
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpStatusCode
import org.springframework.security.oauth2.client.AuthorizedClientServiceOAuth2AuthorizedClientManager
import org.springframework.security.oauth2.client.OAuth2AuthorizationFailureHandler
import org.springframework.security.oauth2.client.OAuth2AuthorizationSuccessHandler
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProviderBuilder
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository
import org.springframework.security.oauth2.client.web.client.support.OAuth2RestClientHttpServiceGroupConfigurer.from
import org.springframework.web.client.RestClient.ResponseSpec.ErrorHandler
import org.springframework.web.client.support.RestClientHttpServiceGroupConfigurer
import org.zalando.logbook.spring.LogbookClientHttpRequestInterceptor
import org.zalando.logbook.core.HeaderFilters.eachHeader
import no.nav.sikkerhetstjenesten.felles.rest.DefaultRestErrorHandler.Companion.IDENTIFIKATOR
import no.nav.sikkerhetstjenesten.felles.utils.extensions.DomainExtensions.maskFnr
import org.zalando.logbook.HeaderFilter.merge
import org.zalando.logbook.core.HeaderFilters.defaultValue


@Configuration
class ClientSecurityBeanConfig(private val logbookInterceptor: ObjectProvider<LogbookClientHttpRequestInterceptor>){

    @Bean
    fun securityGroupConfigurer(
        manager: OAuth2AuthorizedClientManager,
        interceptors: FellesHttpClientInterceptors,
        handler: ErrorHandler) =
        RestClientHttpServiceGroupConfigurer { groups ->
            from(manager).configureGroups(groups)
            groups.forEachClient { group, builder ->
                builder.requestInterceptors {
                    interceptors.configure(it)
                    if (group.name() == GRAPH) {
                        it.add(headerAddingRequestInterceptor(HEADER_CONSISTENCY_LEVEL))
                    }
                    builder.requestInterceptors { interceptors ->
                        logbookInterceptor.ifAvailable { interceptor ->
                            interceptors.remove(interceptor)
                            interceptors.addLast(interceptor)
                        }
                    }
                }
                builder.defaultStatusHandler(HttpStatusCode::isError, handler::handle)
            }
        }

    @Bean
    fun logbookHeaderFilter() =
        merge(
            defaultValue(),
            eachHeader { name, value ->
                if (name.equals(IDENTIFIKATOR, ignoreCase = true)) {
                    value.maskFnr()
                } else {
                    value
                }
            })

    @Bean
    fun authorizedClientManager(
        repo: ClientRegistrationRepository,
        service: OAuth2AuthorizedClientService,
        successHandler: OAuth2AuthorizationSuccessHandler,
        failureHandler: OAuth2AuthorizationFailureHandler) =
        AuthorizedClientServiceOAuth2AuthorizedClientManager(
            repo, service).apply {
            setAuthorizedClientProvider(
                OAuth2AuthorizedClientProviderBuilder.builder()
                    .authorizationCode()
                    .refreshToken()
                    .clientCredentials()
                    .build())
            setAuthorizationSuccessHandler(successHandler)
            setAuthorizationFailureHandler(failureHandler)
        }

    companion object {
        val HEADER_CONSISTENCY_LEVEL = "ConsistencyLevel" to "eventual"
    }
}

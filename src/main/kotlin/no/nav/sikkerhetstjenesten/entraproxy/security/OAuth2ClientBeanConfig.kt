package no.nav.sikkerhetstjenesten.entraproxy.security

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.oauth2.client.AuthorizedClientServiceOAuth2AuthorizedClientManager
import org.springframework.security.oauth2.client.OAuth2AuthorizationFailureHandler
import org.springframework.security.oauth2.client.OAuth2AuthorizationSuccessHandler
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProviderBuilder
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository
import org.springframework.security.oauth2.client.web.client.OAuth2ClientHttpRequestInterceptor
import org.springframework.security.oauth2.client.web.client.support.OAuth2RestClientHttpServiceGroupConfigurer
import org.springframework.web.client.support.RestClientHttpServiceGroupConfigurer

@Configuration
class OAuth2ClientBeanConfig {
    @Bean
    fun oauth2GroupConfigurer(manager: OAuth2AuthorizedClientManager) =
        RestClientHttpServiceGroupConfigurer { groups ->
            OAuth2RestClientHttpServiceGroupConfigurer.from(manager).configureGroups(groups)
            groups.forEachClient { _, builder ->
                builder.requestInterceptors {
                    it.addFirst(OAuth2DownstreamURICapturingInterceptor())
                }
            }
        }

        @Bean
        fun oauth2AuthorizationFailureHandler(service: OAuth2AuthorizedClientService) =
            OAuth2LoggingAuthorizationFailureHandler(OAuth2ClientHttpRequestInterceptor.authorizationFailureHandler(service))

        @Bean
        fun oauth2AuthorizationSuccessHandler(service: OAuth2AuthorizedClientService) =
            OAuth2LoggingAuthorizationSuccessHandler(service) { client, principal, _ ->
                service.saveAuthorizedClient(client, principal)
            }

        @Bean
        fun oauth2AuthorizedClientManager(repo: ClientRegistrationRepository, service: OAuth2AuthorizedClientService, successHandler: OAuth2AuthorizationSuccessHandler, failureHandler: OAuth2AuthorizationFailureHandler) =
            AuthorizedClientServiceOAuth2AuthorizedClientManager(
                repo, service).apply {
                setAuthorizedClientProvider(OAuth2AuthorizedClientProviderBuilder.builder().clientCredentials().build())
                setAuthorizationSuccessHandler(successHandler)
                setAuthorizationFailureHandler(failureHandler)
            }
}
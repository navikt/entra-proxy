package no.nav.sikkerhetstjenesten.entraproxy.security
import no.nav.sikkerhetstjenesten.entraproxy.felles.FellesBeanConfig.Companion.headerAddingRequestInterceptor
import no.nav.sikkerhetstjenesten.entraproxy.felles.OAuth2DownstreamUriCapturingInterceptor
import no.nav.sikkerhetstjenesten.entraproxy.graph.EntraConfig.Companion.GRAPH
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.http.SessionCreationPolicy.STATELESS
import org.springframework.security.oauth2.client.AuthorizedClientServiceOAuth2AuthorizedClientManager
import org.springframework.security.oauth2.client.OAuth2AuthorizationFailureHandler
import org.springframework.security.oauth2.client.OAuth2AuthorizationSuccessHandler
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProviderBuilder
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository
import org.springframework.security.oauth2.client.web.client.OAuth2ClientHttpRequestInterceptor.authorizationFailureHandler
import org.springframework.security.oauth2.client.web.client.support.OAuth2RestClientHttpServiceGroupConfigurer.from
import org.springframework.security.web.SecurityFilterChain
import org.springframework.web.client.support.RestClientHttpServiceGroupConfigurer

@Configuration
class SecurityBeanConfig {

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain =
        http
            .authorizeHttpRequests {
                it.anyRequest().permitAll()
            }
            .requestCache {
                it.disable()
            }
            .sessionManagement {
                it.sessionCreationPolicy(STATELESS)
            }
            .csrf {
                it.disable()
            }
            .formLogin {
                it.disable()
            }
            .httpBasic {
                it.disable()
            }
            .logout {
                it.disable()
            }
            .build()

    @Bean
    fun oauth2GroupConfigurer(manager: OAuth2AuthorizedClientManager) =
        RestClientHttpServiceGroupConfigurer { groups ->
            from(manager).configureGroups(groups)
            groups.forEachClient { group, builder ->
                builder.requestInterceptors {
                    it.addFirst(OAuth2DownstreamUriCapturingInterceptor())
                    if (group.name() == GRAPH) {
                        it.add(headerAddingRequestInterceptor(HEADER_CONSISTENCY_LEVEL))
                    }
                }

            }
        }

    @Bean
    fun oauth2AuthorizationFailureHandler(service: OAuth2AuthorizedClientService) =
        OAuth2LoggingAuthorizationFailureHandler(authorizationFailureHandler(service))

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

private val HEADER_CONSISTENCY_LEVEL = "ConsistencyLevel" to "eventual"

package no.nav.sikkerhetstjenesten.entraproxy.security

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import no.nav.sikkerhetstjenesten.entraproxy.felles.FellesBeanConfig.Companion.headerAddingRequestInterceptor
import no.nav.sikkerhetstjenesten.entraproxy.graph.EntraGraphClient.Companion.GRAPH
import no.nav.sikkerhetstjenesten.entraproxy.tilgang.EntraController.Companion.API_V1
import no.nav.sikkerhetstjenesten.felles.rest.DownstreamUriCapturingInterceptor
import no.nav.sikkerhetstjenesten.felles.security.AbstractOAuth2JsonAccessDeniedHandler
import no.nav.sikkerhetstjenesten.felles.security.AuthContext
import no.nav.sikkerhetstjenesten.felles.security.OAuth2LoggingAuthorizationFailureHandler
import no.nav.sikkerhetstjenesten.felles.security.OAuth2LoggingAuthorizationSuccessHandler
import no.nav.sikkerhetstjenesten.felles.security.SecurityExtensions.stateless
import no.nav.sikkerhetstjenesten.felles.utils.cluster.ClusterConstants.DEV
import org.springframework.beans.factory.ObjectProvider
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpStatusCode
import org.springframework.http.HttpMethod.GET
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.oauth2.client.AuthorizedClientServiceOAuth2AuthorizedClientManager
import org.springframework.security.oauth2.client.OAuth2AuthorizationFailureHandler
import org.springframework.security.oauth2.client.OAuth2AuthorizationSuccessHandler
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProviderBuilder
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository
import org.springframework.security.oauth2.client.web.client.OAuth2ClientHttpRequestInterceptor.authorizationFailureHandler
import org.springframework.security.oauth2.client.web.client.support.OAuth2RestClientHttpServiceGroupConfigurer.from
import org.springframework.security.web.AuthenticationEntryPoint
import org.springframework.security.web.access.AccessDeniedHandler
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher
import org.springframework.web.client.RestClient.ResponseSpec.ErrorHandler
import org.springframework.web.client.support.RestClientHttpServiceGroupConfigurer
import org.zalando.logbook.spring.LogbookClientHttpRequestInterceptor
import tools.jackson.databind.json.JsonMapper
import java.net.URI


@Configuration
class OAuth2SecurityBeanConfig {

    private val UNPROTECTED_ENDPOINTS = PathPatternRequestMatcher.withDefaults().let {
        arrayOf(
            it.matcher("/$DEV/**"),
            it.matcher("/swagger-ui/**"),
            it.matcher("/v3/api-docs/**"),
            it.matcher("/monitoring/**"),
            it.matcher("/grupper.html"),
            it.matcher("/ansatt.html"),
            it.matcher("/utforsker.html"),
            it.matcher(GET, "$API_V1/ansatt/{navIdent}"),
            it.matcher(GET, "$API_V1/ansatt/tilganger/{navIdent}"),
            it.matcher(GET, "$API_V1/enhet/ansatt/{navIdent}"),
            it.matcher(GET, "$API_V1/enhet/{enhetsnummer}"),
            it.matcher(GET, "$API_V1/gruppe/medlemmer")
        )
    }


    @Bean
    fun authContext() = AuthContext()

    @Bean
    fun oauth2JsonAccessDeniedHandler(mapper: JsonMapper, ctx: AuthContext) =
        object : AbstractOAuth2JsonAccessDeniedHandler(mapper, ctx, TYPE_URI) {
            override fun preHandle(req: HttpServletRequest, res: HttpServletResponse) = Unit
        }

    @Bean
    fun securityFilterChain(http: HttpSecurity,
                            converter: OAuth2AuthorityAndRoleAddingJwtAuthenticationConverter,
                            deniedHandler: AccessDeniedHandler,
                            entryPoint: AuthenticationEntryPoint) =
        http.authorizeHttpRequests { requests ->
            requests.requestMatchers(*UNPROTECTED_ENDPOINTS).permitAll()
            requests.anyRequest().authenticated()
        }
            .exceptionHandling {
                it.accessDeniedHandler(deniedHandler)
            }
            .oauth2ResourceServer { oauth2 ->
                oauth2.jwt { jwt ->
                    jwt.jwtAuthenticationConverter(converter)
                }
                oauth2.authenticationEntryPoint(entryPoint)
            }
            .stateless()
            .build()


    @Bean
    fun oauth2GroupConfigurer(manager: OAuth2AuthorizedClientManager,  logbookInterceptor: ObjectProvider<LogbookClientHttpRequestInterceptor>, handler: ErrorHandler) =
        RestClientHttpServiceGroupConfigurer { groups ->
            from(manager).configureGroups(groups)
            groups.forEachClient { group, builder ->
                builder.requestInterceptors {
                    logbookInterceptor.ifAvailable {
                        interceptor -> it.add(interceptor)
                    }
                    it.addFirst(DownstreamUriCapturingInterceptor())
                    if (group.name() == GRAPH) {
                        it.add(headerAddingRequestInterceptor(HEADER_CONSISTENCY_LEVEL))
                    }
                }
                builder.defaultStatusHandler(HttpStatusCode::isError, handler::handle)
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


val TYPE_URI = URI.create("https://nav.no/sikkerhetstjenesten/entraproxy/problem")

private val HEADER_CONSISTENCY_LEVEL = "ConsistencyLevel" to "eventual"

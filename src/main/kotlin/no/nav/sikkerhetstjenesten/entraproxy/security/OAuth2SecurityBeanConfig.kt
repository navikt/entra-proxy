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
import org.slf4j.LoggerFactory
import org.slf4j.LoggerFactory.getLogger
import org.springframework.beans.factory.ObjectProvider
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.annotation.Order
import org.springframework.http.HttpMethod.GET
import org.springframework.http.HttpMethod.POST
import org.springframework.http.HttpStatusCode
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper
import org.springframework.security.oauth2.client.AuthorizedClientServiceOAuth2AuthorizedClientManager
import org.springframework.security.oauth2.client.OAuth2AuthorizationFailureHandler
import org.springframework.security.oauth2.client.OAuth2AuthorizationSuccessHandler
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProviderBuilder
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository
import org.springframework.security.oauth2.client.web.client.OAuth2ClientHttpRequestInterceptor.authorizationFailureHandler
import org.springframework.security.oauth2.client.web.client.support.OAuth2RestClientHttpServiceGroupConfigurer.from
import org.springframework.security.oauth2.core.oidc.user.OidcUserAuthority
import org.springframework.security.web.AuthenticationEntryPoint
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.access.AccessDeniedHandler
import org.springframework.security.web.authentication.AuthenticationFailureHandler
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher
import org.springframework.security.web.util.matcher.OrRequestMatcher
import org.springframework.web.client.RestClient.ResponseSpec.ErrorHandler
import org.springframework.web.client.support.RestClientHttpServiceGroupConfigurer
import org.zalando.logbook.spring.LogbookClientHttpRequestInterceptor
import tools.jackson.databind.json.JsonMapper
import java.net.URI


@Configuration
class OAuth2SecurityBeanConfig(
    @param:Value($$"${groups.utforsker:}") private val utforskerGruppeId: String,
) {

    private val log = getLogger(javaClass)

    private val UTFORSKER_API_ENDPOINTS = PathPatternRequestMatcher.withDefaults().let {
        arrayOf(
            it.matcher(GET, "$API_V1/ansatt/{navIdent}"),
            it.matcher(GET, "$API_V1/ansatt/tilganger/{navIdent}"),
            it.matcher(GET, "$API_V1/gruppe/medlemmer"),
            it.matcher(GET, "$API_V1/gruppe/antall"),
            it.matcher(GET, "$API_V1/nom/enhet/{navIdent}"),
            it.matcher(GET, "$API_V1/nom/ansatte/{tilgangsenhetId}"),
            it.matcher(GET, "$API_V1/nom/ansatte/enhet/{nomId}"),
            it.matcher(POST, "$API_V1/nom/enhet/bulk"),
        )
    }

    private val LOGIN_ENDPOINTS = arrayOf(
        "/utforsker.html",
        "/grupper.html",
        "/oauth2/authorization/**",
        "/login/oauth2/code/**",
        "/logout",
    )

    private val innloggingFeiletHandler = AuthenticationFailureHandler { _, response, exception ->
        log.warn("Interaktiv innlogging feilet: {}", exception.message, exception)
        response.sendRedirect("/innlogging-feilet.html")
    }

    private val manglerTilgangHandler = AccessDeniedHandler { _, response, exception ->
        log.warn("Bruker mangler påkrevd gruppetilgang til utforsker: {}", exception.message)
        response.sendRedirect("/mangler-tilgang.html")
    }

    private val gruppeAutoriteter = GrantedAuthoritiesMapper { authorities ->
        buildSet {
            addAll(authorities)
            authorities.filterIsInstance<OidcUserAuthority>().forEach { autoritet ->
                autoritet.idToken.getClaimAsStringList(GROUPS_CLAIM).orEmpty().forEach {
                    add(SimpleGrantedAuthority("$GRUPPE_PREFIX$it"))
                }
            }
        }
    }

    @Bean
    fun authContext() = AuthContext()

    @Bean
    fun oauth2JsonAccessDeniedHandler(mapper: JsonMapper, ctx: AuthContext) =
        object : AbstractOAuth2JsonAccessDeniedHandler(mapper, ctx, TYPE_URI) {
            override fun preHandle(req: HttpServletRequest, res: HttpServletResponse) = Unit
        }

    @Bean
    @Order(1)
    fun utforskerApiSecurityFilterChain(http: HttpSecurity,
                                        converter: OAuth2AuthorityAndRoleAddingJwtAuthenticationConverter,
                                        deniedHandler: AccessDeniedHandler,
                                        entryPoint: AuthenticationEntryPoint): SecurityFilterChain =
        http.securityMatcher(OrRequestMatcher(*UTFORSKER_API_ENDPOINTS))
            .authorizeHttpRequests { requests ->
                requests.anyRequest().authenticated()
            }
            .csrf { it.disable() }
            .oauth2ResourceServer { oauth2 ->
                oauth2.jwt { jwt ->
                    jwt.jwtAuthenticationConverter(converter)
                }
                oauth2.authenticationEntryPoint(entryPoint)
            }
            .exceptionHandling {
                it.authenticationEntryPoint(entryPoint)
                it.accessDeniedHandler(deniedHandler)
            }
            .build()

    @Bean
    @Order(2)
    fun authorizationCodeLoginSecurityFilterChain(http: HttpSecurity): SecurityFilterChain =
        http.securityMatcher(*LOGIN_ENDPOINTS)
            .authorizeHttpRequests { requests ->
                if (utforskerGruppeId.isNotBlank()) {
                  //  requests.requestMatchers("/utforsker.html", "/grupper.html").hasAuthority("$GRUPPE_PREFIX$utforskerGruppeId")
                }
                requests.anyRequest().authenticated()
            }
            .oauth2Login { login ->
                login.defaultSuccessUrl("/utforsker.html")
                login.failureHandler(innloggingFeiletHandler)
                login.userInfoEndpoint { it.userAuthoritiesMapper(gruppeAutoriteter) }
            }
            .exceptionHandling { it.accessDeniedHandler(manglerTilgangHandler) }
            .logout { logout ->
                logout.logoutRequestMatcher(PathPatternRequestMatcher.withDefaults().matcher(GET, "/logout"))
                logout.logoutSuccessUrl("/logout.html")
            }
            .build()

    @Bean
    @Order(3)
    fun securityFilterChain(http: HttpSecurity,
                            converter: OAuth2AuthorityAndRoleAddingJwtAuthenticationConverter,
                            deniedHandler: AccessDeniedHandler,
                            entryPoint: AuthenticationEntryPoint) =
        http.authorizeHttpRequests { requests ->
            requests.requestMatchers(PathPatternRequestMatcher.withDefaults().matcher("$API_V1/**")).authenticated()
            requests.anyRequest().permitAll()
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
            setAuthorizedClientProvider(
                OAuth2AuthorizedClientProviderBuilder.builder()
                    .authorizationCode()
                    .refreshToken()
                    .clientCredentials()
                    .build())
            setAuthorizationSuccessHandler(successHandler)
            setAuthorizationFailureHandler(failureHandler)
        }
}


val TYPE_URI = URI.create("https://nav.no/sikkerhetstjenesten/entraproxy/problem")

private const val GROUPS_CLAIM = "groups"
private const val GRUPPE_PREFIX = "GRUPPE_"

private val HEADER_CONSISTENCY_LEVEL = "ConsistencyLevel" to "eventual"

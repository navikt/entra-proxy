package no.nav.sikkerhetstjenesten.entraproxy.security

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import no.nav.sikkerhetstjenesten.entraproxy.felles.FellesBeanConfig.Companion.headerAddingRequestInterceptor
import no.nav.sikkerhetstjenesten.entraproxy.graph.EntraGraphClient.Companion.GRAPH
import no.nav.sikkerhetstjenesten.entraproxy.security.OAuth2AuthorityAndRoleAddingJwtAuthenticationConverter.Companion.GROUPS_CLAIM
import no.nav.sikkerhetstjenesten.entraproxy.security.OAuth2AuthorityAndRoleAddingJwtAuthenticationConverter.Companion.GRUPPE_PREFIX
import no.nav.sikkerhetstjenesten.entraproxy.tilgang.EntraController.Companion.API_V1
import no.nav.sikkerhetstjenesten.felles.rest.DownstreamUriCapturingInterceptor
import no.nav.sikkerhetstjenesten.felles.security.AbstractOAuth2JsonAccessDeniedHandler
import no.nav.sikkerhetstjenesten.felles.security.AuthContext
import no.nav.sikkerhetstjenesten.felles.security.AuthContext.Companion.NAVIDENT
import no.nav.sikkerhetstjenesten.felles.security.OAuth2LoggingAuthorizationFailureHandler
import no.nav.sikkerhetstjenesten.felles.security.OAuth2LoggingAuthorizationSuccessHandler
import no.nav.sikkerhetstjenesten.felles.security.SecurityExtensions.stateless
import org.slf4j.LoggerFactory.getLogger
import org.springframework.beans.factory.ObjectProvider
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.annotation.Order
import org.springframework.core.convert.converter.Converter
import org.springframework.http.HttpMethod.GET
import org.springframework.http.HttpStatusCode
import org.springframework.security.authentication.AbstractAuthenticationToken
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
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper
import org.springframework.security.core.context.SecurityContextHolder.getContext
import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal
import org.springframework.security.oauth2.core.oidc.user.OidcUserAuthority
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.web.AuthenticationEntryPoint
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.access.AccessDeniedHandler
import org.springframework.security.web.authentication.AuthenticationFailureHandler
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher.withDefaults
import org.springframework.web.client.RestClient.ResponseSpec.ErrorHandler
import org.springframework.web.client.support.RestClientHttpServiceGroupConfigurer
import org.zalando.logbook.spring.LogbookClientHttpRequestInterceptor
import tools.jackson.databind.json.JsonMapper
import java.net.URI
import java.net.URLEncoder
import kotlin.text.Charsets.UTF_8


@Configuration
class OAuth2SecurityBeanConfig( private val converter:  Converter<Jwt, AbstractAuthenticationToken>) {

    private val log = getLogger(javaClass)


    @Bean
    @Qualifier(UTFORSKER_MANGLER)
    fun manglerTilgangHandler() = AccessDeniedHandler { _, response, e ->
        val navIdent = (getContext().authentication?.principal as? OAuth2AuthenticatedPrincipal)
            ?.getAttribute<String>(NAVIDENT)
        log.warn("Bruker {} mangler påkrevd gruppetilgang til utforsker: {}", navIdent, e.message)
        val query = navIdent?.let { "?navident=${URLEncoder.encode(it, UTF_8)}" }.orEmpty()
        response.sendRedirect("/mangler-tilgang.html$query")
    }

    @Bean
    fun authContext() =
        AuthContext()

    @Bean
    @Qualifier(API_MANGLER)
    fun oauth2JsonAccessDeniedHandler(mapper: JsonMapper,
                                      ctx: AuthContext) =
        object : AbstractOAuth2JsonAccessDeniedHandler(mapper, ctx, TYPE_URI) {
            override fun preHandle(req: HttpServletRequest, res: HttpServletResponse) = Unit
        }

    @Bean
    fun utforskerAuthoritiesMapper(): GrantedAuthoritiesMapper =
        GrantedAuthoritiesMapper { authorities ->
            buildSet<GrantedAuthority> {
                addAll(authorities)
                authorities.filterIsInstance<OidcUserAuthority>().forEach { oidc ->
                    oidc.idToken.getClaimAsStringList(GROUPS_CLAIM).orEmpty().forEach {
                        add(SimpleGrantedAuthority("$GRUPPE_PREFIX$it"))
                    }
                }
            }
        }

    @Bean
    @Order(1)
    fun utforskerSecurityFilterChain(http: HttpSecurity,
                                     @Qualifier(UTFORSKER_MANGLER) handler: AccessDeniedHandler,
                                     authoritiesMapper: GrantedAuthoritiesMapper,
                                     @Value($$"${groups.utforsker:}")  utforskerGruppe: String): SecurityFilterChain =
        http.securityMatcher(*LOGIN_ENDEPUNKTER, *UTFORSKER_SIDER)
            .authorizeHttpRequests { requests ->
                requests.requestMatchers(*LOGIN_ENDEPUNKTER).permitAll()
                with(requests.anyRequest()) {
                    if (utforskerGruppe.isNotBlank())  {
                        hasAuthority("$GRUPPE_PREFIX$utforskerGruppe")
                    }
                    else {
                        authenticated()
                    }
                }
            }
            .oauth2Login { login ->
                login.loginPage(INNLOGGING)
                login.failureHandler(innloggingFeiletHandler())
                login.userInfoEndpoint { it.userAuthoritiesMapper(authoritiesMapper) }
            }
            .logout { logout ->
                logout.logoutRequestMatcher(withDefaults().matcher(GET, LOGOUT_ENDEPUNKT))
                logout.logoutSuccessUrl(LOGOUT_SIDE)
            }
            .exceptionHandling { it.accessDeniedHandler(handler) }
            .build()

    private fun innloggingFeiletHandler(): AuthenticationFailureHandler {
        val delegate = SimpleUrlAuthenticationFailureHandler(INNLOGGING_FEILET)
        return AuthenticationFailureHandler { request, response, exception ->
            log.warn("Innlogging feilet: {}", exception.message, exception)
            delegate.onAuthenticationFailure(request, response, exception)
        }
    }

    @Bean
    @Order(2)
    fun apiSecurityFilterChain(http: HttpSecurity,
                               entryPoint: AuthenticationEntryPoint,
                               @Qualifier(API_MANGLER) handler: AccessDeniedHandler)  =
        http.authorizeHttpRequests { requests ->
                requests.requestMatchers(API_ENDPOINTS).authenticated()
                requests.anyRequest().permitAll()
            }
            .asResourceServer(converter, entryPoint, handler)

    @Bean
    fun oauth2GroupConfigurer(manager: OAuth2AuthorizedClientManager, logbook: ObjectProvider<LogbookClientHttpRequestInterceptor>, handler: ErrorHandler) =
        RestClientHttpServiceGroupConfigurer { groups ->
            from(manager).configureGroups(groups)
            groups.forEachClient { group, builder ->
                builder.requestInterceptors {
                    logbook.ifAvailable {
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

    private fun HttpSecurity.asResourceServer(converter: Converter<Jwt, AbstractAuthenticationToken>, entry: AuthenticationEntryPoint, handler: AccessDeniedHandler) =
        oauth2ResourceServer {
            it.jwt { jwt ->
                jwt.jwtAuthenticationConverter(converter)
            }
            it.authenticationEntryPoint(entry)
        }
            .exceptionHandling {
                it.authenticationEntryPoint(entry)
                it.accessDeniedHandler(handler)
            }
            .stateless().build()


    companion object {
        private const val UTFORSKER_MANGLER = "mangler"
        private const val API_MANGLER = "json"
        private const val LOGOUT_SIDE = "/logout.html"
        private const val LOGOUT_ENDEPUNKT = "/oauth2/logout"
        private const val INNLOGGING = "/oauth2/authorization/entra"
        private const val INNLOGGING_FEILET = "/innlogging-feilet.html"
        private val API_ENDPOINTS = withDefaults().matcher("$API_V1/**")
        private val UTFORSKER_SIDER = arrayOf("/", "/index.html", "/grupper.html", "/ansatt.html")
        private val LOGIN_ENDEPUNKTER = arrayOf("/oauth2/authorization/**", "/login/oauth2/code/**", LOGOUT_ENDEPUNKT)
        val TYPE_URI = URI.create("https://nav.no/sikkerhetstjenesten/entraproxy/problem")
        val HEADER_CONSISTENCY_LEVEL = "ConsistencyLevel" to "eventual"
    }
}





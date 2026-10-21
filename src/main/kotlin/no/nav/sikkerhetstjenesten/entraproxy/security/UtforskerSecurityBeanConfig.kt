package no.nav.sikkerhetstjenesten.entraproxy.security

import no.nav.sikkerhetstjenesten.entraproxy.security.ApiAuthorityAndRoleAddingJwtAuthenticationConverter.Companion.GROUPS_CLAIM
import no.nav.sikkerhetstjenesten.entraproxy.security.ApiAuthorityAndRoleAddingJwtAuthenticationConverter.Companion.GRUPPE_PREFIX
import no.nav.sikkerhetstjenesten.entraproxy.tilgang.EntraController.Companion.API_V1
import no.nav.sikkerhetstjenesten.felles.security.AuthContext.Companion.NAVIDENT
import org.slf4j.LoggerFactory.getLogger
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.annotation.Order
import org.springframework.http.HttpHeaders.AUTHORIZATION
import org.springframework.http.HttpMethod.GET
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper
import org.springframework.security.core.context.SecurityContextHolder.getContext
import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal
import org.springframework.security.oauth2.core.OAuth2AuthenticationException
import org.springframework.security.oauth2.core.oidc.user.OidcUserAuthority
import org.springframework.security.web.AuthenticationEntryPoint
import org.springframework.security.web.access.AccessDeniedHandler
import org.springframework.security.web.authentication.AuthenticationFailureHandler
import org.springframework.security.web.authentication.DelegatingAuthenticationEntryPoint
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher.withDefaults
import org.springframework.security.web.util.matcher.AndRequestMatcher
import org.springframework.security.web.util.matcher.NegatedRequestMatcher
import org.springframework.security.web.util.matcher.OrRequestMatcher
import org.springframework.security.web.util.matcher.RequestMatcher
import java.net.URLEncoder.encode
import kotlin.text.Charsets.UTF_8


@Configuration
class UtforskerSecurityBeanConfig {

    private val log = getLogger(javaClass)

    @Bean
    @Order(1)
    fun utforskerSecurityFilterChain(http: HttpSecurity,
                                     @Qualifier(UTFORSKER_HANDLER) handler: AccessDeniedHandler,
                                     entryPoint: AuthenticationEntryPoint,
                                     authoritiesMapper: GrantedAuthoritiesMapper,
                                     @Value($$"${groups.utforsker:}")  utforskerGruppe: String)  =
        http.securityMatcher(utforskerSesjonMatcher())
            .csrf { it.ignoringRequestMatchers(API_ENDPOINTS) }
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
                login.failureHandler(utforskerInnloggingFeiletHandler())
                login.userInfoEndpoint {
                    it.oidcUserService(utforskerBrukerTjeneste())
                    it.userAuthoritiesMapper(authoritiesMapper)
                }
            }
            .logout { logout ->
                logout.logoutRequestMatcher(withDefaults().matcher(GET, LOGOUT_ENDEPUNKT))
                logout.logoutSuccessUrl(LOGOUT_SIDE)
            }
            .exceptionHandling {
                it.authenticationEntryPoint(utforskerSesjonEntryPoint(entryPoint))
                it.accessDeniedHandler(handler)
            }
            .build()

    @Bean
    @Qualifier(UTFORSKER_HANDLER)
    fun utforskerManglendeTilgangHandler() = AccessDeniedHandler { _, response, e ->
        val navIdent = (getContext().authentication?.principal as? OAuth2AuthenticatedPrincipal)
            ?.getAttribute<String>(NAVIDENT)
        log.warn("Bruker {} mangler påkrevd gruppetilgang til utforsker: {}", navIdent, e.message)
        val query = navIdent?.let { "?navident=${encode(it, UTF_8)}" }.orEmpty()
        response.sendRedirect("/mangler-tilgang.html$query")
    }



    @Bean
    fun utforskerAuthoritiesMapper()  =
        GrantedAuthoritiesMapper { authorities ->
            buildSet {
                addAll(authorities)
                authorities.filterIsInstance<OidcUserAuthority>().forEach { oidc ->
                    oidc.idToken.getClaimAsStringList(GROUPS_CLAIM).orEmpty().forEach {
                        add(SimpleGrantedAuthority("$GRUPPE_PREFIX$it"))
                    }
                }
            }
        }

    private fun utforskerSesjonEntryPoint(apiEntryPoint: AuthenticationEntryPoint)  =
        DelegatingAuthenticationEntryPoint.builder()
            .addEntryPointFor(apiEntryPoint, API_ENDPOINTS)
            .defaultEntryPoint(LoginUrlAuthenticationEntryPoint(INNLOGGING))
            .build()

    private fun utforskerBrukerTjeneste() =
        OidcUserService().apply {
            setRetrieveUserInfo { false }
        }

    private fun utforskerSesjonMatcher() =
        OrRequestMatcher(
            buildList {
                (LOGIN_ENDEPUNKTER + UTFORSKER_SIDER).forEach {
                    add(withDefaults().matcher(it))
                }
                add(BROWSER_API_ENDPOINTS)
            })

    private fun utforskerInnloggingFeiletHandler(): AuthenticationFailureHandler =
        AuthenticationFailureHandler { _, response, e ->
            val årsak = (e as? OAuth2AuthenticationException)?.error?.errorCode ?: "ukjent"
            log.warn("Innlogging feilet [{}]: {}", årsak, e.message, e)
            response.sendRedirect("$INNLOGGING_FEILET?arsak=${encode(årsak, UTF_8)}")
        }

    companion object {
        private const val UTFORSKER_HANDLER = "utforskerHandler"
        private const val LOGOUT_SIDE = "/logout.html"
        private const val LOGOUT_ENDEPUNKT = "/oauth2/logout"
        private const val INNLOGGING = "/oauth2/authorization/entra"
        private const val INNLOGGING_FEILET = "/innlogging-feilet.html"
        internal val API_ENDPOINTS: RequestMatcher = withDefaults().matcher("$API_V1/**")
        private const val BEARER_PREFIX = "Bearer "
        private val BROWSER_API_ENDPOINTS = AndRequestMatcher(API_ENDPOINTS,
            NegatedRequestMatcher { it.getHeader(AUTHORIZATION)?.startsWith(BEARER_PREFIX) == true })
        private val UTFORSKER_SIDER = arrayOf("/", "/index.html", "/grupper.html", "/ansatt.html")
        private val LOGIN_ENDEPUNKTER = arrayOf("/oauth2/authorization/**", "/login/oauth2/code/**", LOGOUT_ENDEPUNKT)
    }
}





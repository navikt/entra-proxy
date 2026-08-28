package no.nav.sikkerhetstjenesten.entraproxy.security

import io.opentelemetry.api.trace.Span
import no.nav.sikkerhetstjenesten.entraproxy.felles.utils.cluster.ClusterConstants.DEV
import org.springframework.beans.factory.ObjectProvider
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.convert.converter.Converter
import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail.forStatusAndDetail
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.authentication.AbstractAuthenticationToken
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.http.SessionCreationPolicy.STATELESS
import org.springframework.security.config.observation.SecurityObservationSettings
import org.springframework.security.oauth2.client.AuthorizedClientServiceOAuth2AuthorizedClientManager
import org.springframework.security.oauth2.client.OAuth2AuthorizationFailureHandler
import org.springframework.security.oauth2.client.OAuth2AuthorizationSuccessHandler
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProviderBuilder
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository
import org.springframework.security.oauth2.client.web.client.OAuth2ClientHttpRequestInterceptor.authorizationFailureHandler
import org.springframework.security.oauth2.client.web.client.support.OAuth2RestClientHttpServiceGroupConfigurer.from
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.web.AuthenticationEntryPoint
import org.springframework.security.web.access.AccessDeniedHandler
import org.springframework.web.client.support.RestClientHttpServiceGroupConfigurer
import java.net.URI
import java.time.ZoneId
import kotlin.annotation.AnnotationRetention.RUNTIME
import kotlin.annotation.AnnotationTarget.CLASS
import kotlin.annotation.AnnotationTarget.FUNCTION

private val UNPROTECTED_ENDPOINTS = arrayOf("/$DEV/**", "/swagger-ui/**", "/v3/api-docs/**", "/monitoring/**")

@Configuration
@EnableMethodSecurity
class OAuth2SecurityBeanConfig {
    @Bean
    fun securityFilterChain(http: HttpSecurity,
                            converter: ObjectProvider<Converter<Jwt, AbstractAuthenticationToken>>,
                            deniedHandler: AccessDeniedHandler,
                            entryPoint: AuthenticationEntryPoint) =
        http.authorizeHttpRequests { requests ->
            requests.requestMatchers( *UNPROTECTED_ENDPOINTS).permitAll()
            requests.anyRequest().authenticated()
        }.exceptionHandling {
            it.accessDeniedHandler(deniedHandler)
        }.oauth2ResourceServer { oauth2 ->
            oauth2.jwt { jwt ->
                converter.ifAvailable?.let(jwt::jwtAuthenticationConverter)
            }
            oauth2.authenticationEntryPoint(entryPoint)
        }.stateless()
            .build()

    @Bean
    fun securityObservationSettings()  =
        SecurityObservationSettings.withDefaults().shouldObserveRequests(false)
            .build()

    private fun HttpSecurity.stateless() =
        requestCache { it.disable() }
            .sessionManagement { it.sessionCreationPolicy(STATELESS) }
            .csrf { it.disable() }
            .formLogin { it.disable() }
            .httpBasic { it.disable() }
            .logout { it.disable() }
}


internal const val FEIL_AUDIENCE = "An error occurred while attempting to decode the Jwt: The aud claim is not valid"
internal const val MANGLER_BEARER_TOKEN = "Bruker er ikke logget inn. Mangler Bearer token i Authorization header."
val TYPE_URI = URI.create("https://confluence.adeo.no/display/TM/Tilgangsmaskin+API+og+regelsett")
val OSLO = ZoneId.of("Europe/Oslo")
internal fun securityProblemDetail(status: HttpStatus, detail: String) =
    forStatusAndDetail(status, detail).apply {
        type = TYPE_URI
        title = "${status.value()}"
        properties = mapOf("traceId" to Span.current().spanContext.traceId)
    }

@Target(CLASS, FUNCTION)
@Retention(RUNTIME)
@PreAuthorize("@tokenTypeAuthorization.require(T(no.nav.sikkerhetstjenesten.entraproxy.felles.rest.TokenType).CCF)")
annotation class OAuth2RequireCCF

@Target(CLASS, FUNCTION)
@Retention(RUNTIME)
@PreAuthorize("@tokenTypeAuthorization.require(T(no.nav.sikkerhetstjenesten.entraproxy.felles.rest.TokenType).OBO)")
annotation class OAuth2RequireOBO


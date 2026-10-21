package no.nav.sikkerhetstjenesten.entraproxy.security

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import no.nav.sikkerhetstjenesten.entraproxy.security.UtforskerSecurityBeanConfig.Companion.API_ENDPOINTS
import no.nav.sikkerhetstjenesten.felles.security.AbstractOAuth2JsonAccessDeniedHandler
import no.nav.sikkerhetstjenesten.felles.security.AuthContext
import no.nav.sikkerhetstjenesten.felles.security.SecurityExtensions.stateless
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.annotation.Order
import org.springframework.core.convert.converter.Converter
import org.springframework.security.authentication.AbstractAuthenticationToken
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.web.AuthenticationEntryPoint
import org.springframework.security.web.access.AccessDeniedHandler
import tools.jackson.databind.json.JsonMapper
import java.net.URI


@Configuration
class ApiSecurityBeanConfig(private val converter: Converter<Jwt, AbstractAuthenticationToken>) {

    @Bean
    @Order(2)
    fun apiSecurityFilterChain(http: HttpSecurity,
                               entryPoint: AuthenticationEntryPoint,
                               @Qualifier(API_MANGLER) handler: AccessDeniedHandler) =
        http.authorizeHttpRequests { requests ->
            requests.requestMatchers(API_ENDPOINTS).authenticated()
            requests.anyRequest().permitAll()
        }
            .oauth2ResourceServer {
                it.jwt { jwt ->
                    jwt.jwtAuthenticationConverter(converter)
                }
                it.authenticationEntryPoint(entryPoint)
            }
            .exceptionHandling {
                it.authenticationEntryPoint(entryPoint)
                it.accessDeniedHandler(handler)
            }
            .stateless().build()

    @Bean
    @Qualifier(API_MANGLER)
    fun oauth2JsonAccessDeniedHandler(mapper: JsonMapper, ctx: AuthContext) =
        object : AbstractOAuth2JsonAccessDeniedHandler(mapper, ctx, TYPE_URI) {
            override fun preHandle(req: HttpServletRequest, res: HttpServletResponse) = Unit
        }

    companion object {
        private const val API_MANGLER = "apiMangler"
        private val TYPE_URI = URI.create("https://nav.no/sikkerhetstjenesten/entraproxy/problem")
    }
}

package no.nav.sikkerhetstjenesten.entraproxy.tilgang

import no.nav.sikkerhetstjenesten.entraproxy.felles.notifikasjon.LogbookBeanConfiguration
import no.nav.sikkerhetstjenesten.entraproxy.felles.rest.AuthContext
import no.nav.sikkerhetstjenesten.entraproxy.felles.rest.DefaultRestErrorHandler
import no.nav.sikkerhetstjenesten.entraproxy.security.OAuth2AuthorityAndRoleAddingJwtAuthenticationConverter
import no.nav.sikkerhetstjenesten.entraproxy.security.OAuth2JsonAccessDeniedHandler
import no.nav.sikkerhetstjenesten.entraproxy.security.OAuth2JsonAuthenticationEntryPoint
import no.nav.sikkerhetstjenesten.entraproxy.security.OAuth2SecurityBeanConfig
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.Import

@SpringBootApplication
@Import(
    EntraController::class,
    AuthContext::class,
    OAuth2AuthorityAndRoleAddingJwtAuthenticationConverter::class,
    OAuth2JsonAccessDeniedHandler::class,
    OAuth2JsonAuthenticationEntryPoint::class,
    OAuth2SecurityBeanConfig::class,
    LogbookBeanConfiguration::class,
    DefaultRestErrorHandler::class)
class SecurityTestApplication
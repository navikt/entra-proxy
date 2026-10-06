package no.nav.sikkerhetstjenesten.entraproxy.tilgang

import no.nav.sikkerhetstjenesten.entraproxy.security.OAuth2AuthorityAndRoleAddingJwtAuthenticationConverter
import no.nav.sikkerhetstjenesten.entraproxy.security.OAuth2JsonAuthenticationEntryPoint
import no.nav.sikkerhetstjenesten.entraproxy.security.OAuth2SecurityBeanConfig
import no.nav.sikkerhetstjenesten.felles.notifikasjon.NotificationAutoConfiguration
import no.nav.sikkerhetstjenesten.felles.rest.DefaultRestErrorHandler
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.Import

@SpringBootApplication
@Import(
    EntraController::class,
    OAuth2AuthorityAndRoleAddingJwtAuthenticationConverter::class,
    OAuth2JsonAuthenticationEntryPoint::class,
    OAuth2SecurityBeanConfig::class,
    NotificationAutoConfiguration::class,
    DefaultRestErrorHandler::class)
class SecurityTestApplication
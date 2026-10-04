package no.nav.sikkerhetstjenesten.entraproxy.tilgang

import no.nav.sikkerhetstjenesten.entraproxy.felles.rest.CsvWebMvcConfig
import no.nav.sikkerhetstjenesten.felles.security.AuthContext as LibraryAuthContext
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
    LibraryAuthContext::class,
    OAuth2AuthorityAndRoleAddingJwtAuthenticationConverter::class,
    OAuth2JsonAuthenticationEntryPoint::class,
    OAuth2SecurityBeanConfig::class,
    LibraryAuthContext::class,
    NotificationAutoConfiguration::class,
    DefaultRestErrorHandler::class,
    CsvWebMvcConfig::class)
class SecurityTestApplication
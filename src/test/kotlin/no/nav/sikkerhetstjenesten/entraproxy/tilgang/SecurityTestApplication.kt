package no.nav.sikkerhetstjenesten.entraproxy.tilgang

import no.nav.sikkerhetstjenesten.entraproxy.felles.PageNotFoundHandler
import no.nav.sikkerhetstjenesten.entraproxy.security.ApiAuthenticationEntryPoint
import no.nav.sikkerhetstjenesten.entraproxy.security.ApiAuthorityAndRoleAddingJwtAuthenticationConverter
import no.nav.sikkerhetstjenesten.entraproxy.security.ApiSecurityBeanConfig
import no.nav.sikkerhetstjenesten.entraproxy.security.ClientSecurityBeanConfig
import no.nav.sikkerhetstjenesten.entraproxy.security.UtforskerSecurityBeanConfig
import no.nav.sikkerhetstjenesten.felles.cache.CacheAutoConfiguration
import no.nav.sikkerhetstjenesten.felles.notifikasjon.NotificationAutoConfiguration
import no.nav.sikkerhetstjenesten.felles.rest.DefaultRestErrorHandler
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.Import

@SpringBootApplication(exclude = [CacheAutoConfiguration::class])
@Import(
    EntraController::class,
    PageNotFoundHandler::class,
    ApiAuthorityAndRoleAddingJwtAuthenticationConverter::class,
    ApiAuthenticationEntryPoint::class,
    ApiSecurityBeanConfig::class,
    ClientSecurityBeanConfig::class,
    UtforskerSecurityBeanConfig::class,
    NotificationAutoConfiguration::class,
    DefaultRestErrorHandler::class)
class SecurityTestApplication
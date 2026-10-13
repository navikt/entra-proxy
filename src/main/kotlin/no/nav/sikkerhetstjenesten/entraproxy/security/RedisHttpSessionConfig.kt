package no.nav.sikkerhetstjenesten.entraproxy.security

import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession

/**
 * Deler HTTP-sesjonen i Redis (valkey) på tvers av podder i gcp.
 *
 * Authorization code-innloggingen lagrer autorisasjonsforespørselen (state/PKCE) og den
 * innloggede sikkerhetskonteksten i HTTP-sesjonen. Med flere replicas og uten delt sesjon
 * ville callback-forespørselen kunne treffe en annen pod enn den som startet innloggingen,
 * som gir «authorization_request_not_found» og en 404 etter innlogging.
 *
 * Lokalt og i test kjører appen som én instans, og bruker standard in-memory-sesjon.
 */
@Configuration
@Profile("gcp")
@EnableRedisHttpSession
class RedisHttpSessionConfig

package no.nav.sikkerhetstjenesten.entraproxy.security

import no.nav.sikkerhetstjenesten.entraproxy.felles.rest.Token
import no.nav.sikkerhetstjenesten.entraproxy.security.Authorities.GRANTED_CCF_AUTHORITY
import no.nav.sikkerhetstjenesten.entraproxy.security.Authorities.GRANTED_OBO_AUTHORITY
import org.slf4j.LoggerFactory.getLogger
import org.springframework.core.convert.converter.Converter
import org.springframework.security.authentication.AbstractAuthenticationToken
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.core.DefaultOAuth2AuthenticatedPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.stereotype.Component

@Component
class OAuth2AuthorityAndRoleAddingJwtAuthenticationConverter : Converter<Jwt, AbstractAuthenticationToken> {

    private val log = getLogger(javaClass)

    private val delegate = JwtAuthenticationConverter()
        .andThen {
            val jwt = it as JwtAuthenticationToken
            val authorities = buildSet {
                addAll(jwt.authorities)
                addAll(roller(jwt.token))
                authority(jwt.token)?.let(::add)
            }
            JwtAuthenticationToken(jwt.token, principal(jwt.token, authorities), authorities)
        }

    override fun convert(jwt: Jwt)  =
        delegate.convert(jwt) ?: error(
            "JWT konvertering feilet for token med subject='${jwt.subject ?: "unknown"}', claimKeys=${jwt.claims.keys}"
        )


    private fun principal(jwt: Jwt, authorities: Set<GrantedAuthority>) =
        DefaultOAuth2AuthenticatedPrincipal(
            jwt.subject ?: jwt.getClaimAsString(Token.NAVIDENT) ?: "unknown",
            jwt.claims, authorities).also {
            log.trace("Principal satt til {} med authorities: {}", it.name, it.authorities)
        }

    private fun authority(jwt: Jwt) =
        when {
            jwt.getClaimAsStringList(ROLES).orEmpty().contains(CLIENT_CREDENTIALS) -> GRANTED_CCF_AUTHORITY
            jwt.getClaimAsString(Token.OID) != null -> GRANTED_OBO_AUTHORITY
            else -> null
        }

    private fun roller(jwt: Jwt) =
        buildSet {
            jwt.getClaimAsStringList(ROLES).orEmpty().forEach { rolle ->
                add(SimpleGrantedAuthority(rolle.takeIf {
                    it.startsWith(ROLLE)
                } ?: "$ROLLE$rolle"))
            }
        }

    companion object {
        private const val ROLLE = "ROLE_"

    }
}


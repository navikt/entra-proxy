package no.nav.sikkerhetstjenesten.entraproxy.felles.rest


import no.nav.sikkerhetstjenesten.entraproxy.felles.utils.extensions.DomainExtensions.UTILGJENGELIG
import no.nav.sikkerhetstjenesten.entraproxy.graph.AnsattId
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class AuthContext {


    val system get() = stringClaim(AZP_NAME)  ?: UTILGJENGELIG
    val oid get() = stringClaim(OID)?.let { runCatching { UUID.fromString(it) }.getOrNull() }
    val ansattId get() = stringClaim(NAVIDENT)?.let { AnsattId(it) }
    private fun stringClaim(name: String): String? =
        when (val principal = SecurityContextHolder.getContext().authentication?.principal) {
            is Jwt -> principal.getClaimAsString(name)
            is OAuth2AuthenticatedPrincipal -> principal.attributes[name]?.toString()
            else -> null
        }
    val clusterAndSystem get() = system.split(":").let { parts ->
        if (parts.size == 3) "${parts[2]}:${parts[0]}" else system
    }


    val oboFields  get() =
        ansattId?.let { id -> oid?.let { o -> id to o } }
            ?: error("ansattId og oid må være satt for OBO")

    val type get() = TokenType.from(this).name.lowercase()
    val systemNavn get() = system.split(":").last()
    val systemAndNs get() = system.split(":").drop(1).joinToString(separator = ":")
    val cluster get() = system.split(":").first()
    val erCC get() = stringClaim(IDTYP) == APP
    val erObo get()  = !erCC && oid != null
    companion object {
        const val APP = "app"
        const val OID = "oid"
        const val IDTYP = "idtyp"
        const val AZP_NAME = "azp_name"
        const val NAVIDENT = "NAVident"
        const val ROLES = "roles"
        const val CLIENT_CREDENTIALS = "client_credentials"
    }
}

enum class TokenType {
    OBO, CCF, UNAUTHENTICATED;

    companion object {
        fun from(token: AuthContext): TokenType = when {
            token.erObo -> OBO
            token.erCC -> CCF
            else -> UNAUTHENTICATED
        }
    }
}
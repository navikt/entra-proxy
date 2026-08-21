package no.nav.sikkerhetstjenesten.entraproxy.felles.rest


import no.nav.sikkerhetstjenesten.entraproxy.felles.rest.TokenType.CCF
import no.nav.sikkerhetstjenesten.entraproxy.felles.rest.TokenType.OBO
import no.nav.sikkerhetstjenesten.entraproxy.felles.rest.TokenType.UNAUTHENTICATED
import no.nav.sikkerhetstjenesten.entraproxy.felles.utils.extensions.DomainExtensions.UTILGJENGELIG
import no.nav.sikkerhetstjenesten.entraproxy.graph.AnsattId
import org.springframework.security.core.context.SecurityContextHolder.getContext
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.stereotype.Component
import java.util.*

@Component
class Token {


    val system get() = stringClaim(AZP_NAME) ?: UTILGJENGELIG
    val oid get() = stringClaim(OID)?.let { runCatching { UUID.fromString(it) }.getOrNull() }
    val ansattId get() = stringClaim(NAVIDENT)?.let { AnsattId(it) }
    private fun stringClaim(name: String) = jwt()?.claims?.get(name)?.toString()
    private fun jwt() = getContext().authentication?.let { authentication ->
        when (val principal = authentication.principal) {
            is Jwt -> principal
            else -> null
        }
    }
    val clusterAndSystem
        get() = system.split(":").let { parts ->
            if (parts.size == 3) "${parts[2]}:${parts[0]}" else system
        }

    val systemNavn get() = system.split(":").last()
    val systemAndNs get() = system.split(":").drop(1).joinToString(separator = ":")
    val cluster get() = system.split(":").first()
    private val erCC get() = stringClaim(IDTYP) == APP
    private val erObo get() = !erCC && oid != null
    val type
        get() = when {
            erObo -> OBO
            erCC -> CCF
            else -> UNAUTHENTICATED
        }

    companion object {
        const val AAD_ISSUER = "azuread"
        const val APP = "app"
        const val OID = "oid"
        const val IDTYP = "idtyp"
        const val AZP_NAME = "azp_name"
        const val NAVIDENT = "NAVident"
    }
}

enum class TokenType {
    OBO, CCF, UNAUTHENTICATED
}
package no.nav.sikkerhetstjenesten.entraproxy.felles.rest


import no.nav.boot.conditionals.Cluster.LOCAL
import no.nav.security.token.support.core.context.TokenValidationContextHolder
import no.nav.sikkerhetstjenesten.entraproxy.graph.AnsattId
import org.springframework.http.HttpStatus.BAD_REQUEST
import org.springframework.stereotype.Component
import org.springframework.web.server.ResponseStatusException
import java.util.*

@Component
class Token(private val contextHolder: TokenValidationContextHolder) {

    val type = TokenType.from(this)
    val system get() = stringClaim(AZP_NAME)  ?: "N/A"
    val oid get() = stringClaim(OID)?.let { UUID.fromString(it) }
    val ansattId get() = stringClaim(NAVIDENT)?.let { AnsattId(it) }
    private fun stringClaim(name: String) = claimSet()?.getStringClaim(name)
    private fun claimSet() = runCatching { contextHolder.getTokenValidationContext().getClaims(AAD_ISSUER) }.getOrNull()
    val clusterAndSystem get() = system.split(":").let { parts ->
        if (parts.size == 3) "${parts[2]}:${parts[0]}" else system
    }

    val systemNavn get() = system.split(":").lastOrNull() ?: "N/A"
    val systemAndNs get() = runCatching { system.split(":").drop(1).joinToString(separator = ":") }.getOrElse { systemNavn }
    val cluster get() = runCatching { system.split(":").first() }.getOrElse { LOCAL.name.lowercase() }
    val erCC get() = stringClaim(IDTYP) == APP
    val erObo get()  = !erCC && oid != null

    fun <T> assert(predikat: Token.() -> Boolean, block: () -> Set<T>): Set<T> {
        if (!this.predikat()) throw ResponseStatusException(BAD_REQUEST, "Feil i token: krever korrekt token-type for å utføre denne operasjonen")
        else return block()
    }
    companion object {
        const val AAD_ISSUER: String = "azuread"
        private const val APP = "app"
        private const val OID = "oid"
        private const val IDTYP = "idtyp"
        private const val AZP_NAME = "azp_name"
        private const val NAVIDENT = "NAVident"
    }
}

enum class TokenType {
    OBO, CCF, UNAUTHENTICATED;

    companion object {
        fun from(token: Token): TokenType = when {
            token.erObo -> OBO
            token.erCC -> CCF
            else -> UNAUTHENTICATED
        }
    }
}
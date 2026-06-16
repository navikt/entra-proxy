package no.nav.sikkerhetstjenesten.entraproxy.felles.rest


import no.nav.boot.conditionals.Cluster.LOCAL
import no.nav.security.token.support.core.context.TokenValidationContextHolder
import no.nav.sikkerhetstjenesten.entraproxy.felles.utils.extensions.DomainExtensions.UTILGJENGELIG
import no.nav.sikkerhetstjenesten.entraproxy.graph.AnsattId
import org.springframework.stereotype.Component
import java.util.*

@Component
class Token(private val contextHolder: TokenValidationContextHolder) {


    val system get() = stringClaim(AZP_NAME)  ?: UTILGJENGELIG
    val oid get() = stringClaim(OID)?.let { runCatching { UUID.fromString(it) }.getOrNull() }
    val ansattId get() = stringClaim(NAVIDENT)?.let { AnsattId(it) }
    private fun stringClaim(name: String) = claimSet()?.getStringClaim(name)
    private fun claimSet() = runCatching { contextHolder.getTokenValidationContext().getClaims(AAD_ISSUER) }.getOrNull()
    val clusterAndSystem get() = system.split(":").let { parts ->
        if (parts.size == 3) "${parts[2]}:${parts[0]}" else system
    }

    fun <T> assert(predikat: Token.() -> Boolean, block: () -> Set<T>): Set<T> {
        require(predikat()) { "Feil i token: krever korrekt token-type for å utføre denne operasjonen " }
        return block()
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
        private const val FLOW = "flow"
        const val AAD_ISSUER: String = "azuread"
        const val APP = "app"
        const val OID = "oid"
        const val IDTYP = "idtyp"
        const val AZP_NAME = "azp_name"
        const val NAVIDENT = "NAVident"
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
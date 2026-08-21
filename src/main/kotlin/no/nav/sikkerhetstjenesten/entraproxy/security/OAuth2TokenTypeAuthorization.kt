package no.nav.sikkerhetstjenesten.entraproxy.security

import no.nav.sikkerhetstjenesten.entraproxy.felles.rest.Token
import no.nav.sikkerhetstjenesten.entraproxy.felles.rest.TokenType
import org.springframework.security.access.AccessDeniedException
import org.springframework.stereotype.Component

@Component("tokenTypeAuthorization")
class OAuth2TokenTypeAuthorization(private val token: Token) {
    fun require(required: TokenType) =
        (token.type == required).also {
            if (!it) throw AccessDeniedException(mismatch(required, token.type))
        }

    companion object {
        fun mismatch(required: TokenType, actual: TokenType) = "Forventet ${required.name}-token, fikk ${actual.name}"
    }
}
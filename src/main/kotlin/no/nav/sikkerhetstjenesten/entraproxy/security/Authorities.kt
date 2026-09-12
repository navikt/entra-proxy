package no.nav.sikkerhetstjenesten.entraproxy.security

import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.authority.SimpleGrantedAuthority
import kotlin.annotation.AnnotationRetention.RUNTIME
import kotlin.annotation.AnnotationTarget.CLASS
import kotlin.annotation.AnnotationTarget.FUNCTION

object Authorities {
    private const val PREFIX = "TOKEN_"
    const val OBO_AUTHORITY = "${PREFIX}OBO"
    const val CCF_AUTHORITY = "${PREFIX}CCF"

    val GRANTED_OBO_AUTHORITY = SimpleGrantedAuthority(OBO_AUTHORITY)
    val GRANTED_CCF_AUTHORITY = SimpleGrantedAuthority(CCF_AUTHORITY)

    @Target(CLASS, FUNCTION)
    @Retention(RUNTIME)
    @PreAuthorize("hasAuthority('$CCF_AUTHORITY')")
    annotation class OAuth2RequireCCF

    @Target(CLASS, FUNCTION)
    @Retention(RUNTIME)
    @PreAuthorize("hasAuthority('$OBO_AUTHORITY')")
    annotation class OAuth2RequireOBO
}
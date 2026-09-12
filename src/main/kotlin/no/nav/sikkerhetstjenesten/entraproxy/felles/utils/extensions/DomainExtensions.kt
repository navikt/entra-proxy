package no.nav.sikkerhetstjenesten.entraproxy.felles.utils.extensions

import org.slf4j.MDC

object DomainExtensions {
    const val UTILGJENGELIG = "N/A"

    fun String.maskFnr() =
        when (length) {
            11 -> replaceRange(4, 11, "*******")
            13 -> replaceRange(6, 13, "*******")
            else -> this
        }
}
package no.nav.sikkerhetstjenesten.entraproxy.felles.utils.extensions

import org.slf4j.MDC


object DomainExtensions {
    fun requireDigits(verdi: String, len: Int) {
        require(verdi.all { it.isDigit() }) { "Ugyldig(e) tegn i $verdi, forventet $len siffer" }
        require(verdi.length == len) { "Ugyldig lengde ${verdi.length} for $verdi, forventet $len siffer" }
    }
    inline fun <T> withMDC(key: String, value: String, block: () -> T) =
        try {
            MDC.put(key, value)
            block()
        } finally {
            MDC.remove(key)
        }
}
package no.nav.sikkerhetstjenesten.entraproxy.felles.utils.extensions

import org.slf4j.MDC


object DomainExtensions {
    fun requireDigits(verdi: String, len: Int) {
        require(verdi.all { it.isDigit() }) { "Ugyldig(e) tegn i $verdi, forventet $len siffer" }
        require(verdi.length == len) { "Ugyldig lengde ${verdi.length} for $verdi, forventet $len siffer" }
    }

    fun String.upcase() = this.replaceFirstChar { it.uppercaseChar() }
    fun String.maskFnr() = if (length == 11) replaceRange(6, 11, "*****") else this
    fun String.pluralize(items: Collection<Any>, suffix: String = "er", ingen: String = "ingen"): String =
        when (items.size) {
            0 -> "$ingen $this$suffix"
            1 -> this
            else -> "${items.size} $this$suffix"
        }

    inline fun <T> withMDC(key: String, value: String, block: () -> T) =
        try {
            MDC.put(key, value)
            block()
        } finally {
            MDC.remove(key)
        }
}
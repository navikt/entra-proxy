package no.nav.sikkerhetstjenesten.entraproxy.felles.utils.extensions


object DomainExtensions {
    fun requireDigits(verdi: String, len: Int) {
        require(verdi.all { it.isDigit() }) { "Ugyldig(e) tegn i $verdi, forventet $len siffer" }
        require(verdi.length == len) { "Ugyldig lengde ${verdi.length} for $verdi, forventet $len siffer" }
    }
}
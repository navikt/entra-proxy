package no.nav.sikkerhetstjenesten.entraproxy.ansatt

import com.fasterxml.jackson.annotation.JsonValue
import no.nav.sikkerhetstjenesten.entraproxy.felles.utils.extensions.DomainExtensions.requireDigits


data class Enhetsnummer(@JsonValue val verdi: String) {
    init {
        require(verdi.length == 4) { "Kall uten verdi er malformed" }
    }
}

data class Tema(@JsonValue val verdi: String)
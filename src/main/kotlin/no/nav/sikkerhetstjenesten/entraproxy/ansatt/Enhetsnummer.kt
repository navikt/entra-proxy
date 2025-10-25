package no.nav.sikkerhetstjenesten.entraproxy.ansatt

import com.fasterxml.jackson.annotation.JsonValue
import no.nav.sikkerhetstjenesten.entraproxy.felles.utils.extensions.DomainExtensions.requireDigits


data class Enhetsnummer(@JsonValue val verdi: String) {
    init {
      //  requireDigits(verdi, 4)
    }
}

data class Tema(@JsonValue val verdi: String)
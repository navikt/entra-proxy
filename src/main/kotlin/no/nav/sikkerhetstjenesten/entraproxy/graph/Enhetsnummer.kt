package no.nav.sikkerhetstjenesten.entraproxy.graph

import com.fasterxml.jackson.annotation.JsonValue
import no.nav.sikkerhetstjenesten.entraproxy.felles.utils.extensions.DomainExtensions.requireDigits


data class Enhet(val enhetsnummer: Enhetnummer, val navn: String) {
    data class Enhetnummer(@JsonValue val verdi: String) {
        init {
            requireDigits(verdi,4)
           // require(verdi.length == 4) { "Enhetsnummer må være 4 siffer" }
        }
    }
}

data class Tema(@JsonValue val verdi: String)
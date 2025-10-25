package no.nav.sikkerhetstjenesten.entraproxy.graph

import com.fasterxml.jackson.annotation.JsonValue


data class Enhetsnummer(@JsonValue val verdi: String) {
    init {
        require(verdi.length == 4) { "Enhetsnummer må være 4 siffer" }
    }
}

data class Enhet(val enhetsnummer: Enhetsnummer, val navn: String)

data class Tema(@JsonValue val verdi: String)
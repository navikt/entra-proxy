package no.nav.sikkerhetstjenesten.entraproxy.graph

import com.fasterxml.jackson.annotation.JsonValue


data class Enhet(val enhetsnummer: Enhetnummer, val navn: String) {
    data class Enhetnummer(@JsonValue val verdi: String) {
        init {
            require(verdi.length == 4) { "Enhetsnummer må være 4 siffer" }
        }
    }
}

data class Tema(@JsonValue val verdi: String)
package no.nav.sikkerhetstjenesten.entraproxy.graph

import com.fasterxml.jackson.annotation.JsonValue
import no.nav.sikkerhetstjenesten.entraproxy.felles.utils.extensions.DomainExtensions.requireDigits


data class Enhet(val enhetnummer: Enhetnummer, val navn: String) {
    data class Enhetnummer(@JsonValue val verdi: String) {
        init {
            requireDigits(verdi,4)
        }
    }
}

data class Tema(@JsonValue val verdi: String)
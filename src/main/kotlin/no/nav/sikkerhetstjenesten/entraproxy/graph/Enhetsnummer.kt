package no.nav.sikkerhetstjenesten.entraproxy.graph

import com.fasterxml.jackson.annotation.JsonValue
import no.nav.sikkerhetstjenesten.entraproxy.felles.utils.extensions.DomainExtensions.requireDigits
import no.nav.sikkerhetstjenesten.entraproxy.graph.Tema.Companion.TEMA_PREFIX


data class Enhet(val enhetnummer: Enhetnummer, val navn: String) {

    data class Enhetnummer(@JsonValue val verdi: String) {
        init {
            requireDigits(verdi,4)
        }
        val gruppeNavn = "${ENHET_PREFIX}$verdi"

    }
    companion object {
        const val ENHET_PREFIX = "0000-GA-ENHET_"
    }
}

data class Tema(@JsonValue val verdi: String)  {
    init {
        require(verdi.length == 3) { "Tema må være på tre bokstaver" }
        require(verdi.all { it.isLetter() }) { "Tema kan kun bestå av bokstaver" }
    }
    val gruppeNavn = "${TEMA_PREFIX}$verdi"
    companion object {
        const val TEMA_PREFIX = "0000-GA-TEMA_"
    }
}
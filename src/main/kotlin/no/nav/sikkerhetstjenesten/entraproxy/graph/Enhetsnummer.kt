package no.nav.sikkerhetstjenesten.entraproxy.graph

import com.fasterxml.jackson.annotation.JsonValue
import io.swagger.v3.oas.annotations.media.Schema
import no.nav.sikkerhetstjenesten.entraproxy.felles.utils.extensions.DomainExtensions.requireDigits
import no.nav.sikkerhetstjenesten.entraproxy.graph.Tema.Companion.TEMA_PREFIX


data class Enhet(val enhetnummer: Enhetnummer, val navn: String) {

    @Schema(description = "Enhetsnummer representerer en fire-sifret enhet")
    data class Enhetnummer(@JsonValue val verdi: String) : Comparable<Enhetnummer> {
        init {
            requireDigits(verdi,4)
        }

        override fun compareTo(other: Enhetnummer): Int = verdi.compareTo(other.verdi)

        val gruppeNavn = "${ENHET_PREFIX}$verdi"

    }
    companion object {
        const val ENHET_PREFIX = "0000-GA-ENHET_"
    }
}

@Schema(description = "Tema representerer en trebokstavs temakode")
data class Tema(@JsonValue val verdi: String) : Comparable<Tema>   {
    init {
        require(verdi.length == 3) { "Tema må være på tre bokstaver" }
        require(verdi.all { it.isLetter() }) { "Tema kan kun bestå av bokstaver" }
    }

    override fun compareTo(other: Tema): Int = verdi.compareTo(other.verdi)

    val gruppeNavn = "${TEMA_PREFIX}$verdi"
    companion object {
        const val TEMA_PREFIX = "0000-GA-TEMA_"
    }
}
package no.nav.sikkerhetstjenesten.entraproxy.graph

import com.fasterxml.jackson.annotation.JsonValue
import io.swagger.v3.oas.annotations.media.Schema
import no.nav.sikkerhetstjenesten.entraproxy.felles.utils.extensions.DomainExtensions.requireDigits
import no.nav.sikkerhetstjenesten.entraproxy.graph.Enhet.Companion.ENHET_PREFIX
import no.nav.sikkerhetstjenesten.entraproxy.graph.Tema.Companion.TEMA_PREFIX


data class Enhet(@field:Schema(implementation = Enhetnummer::class) val enhetnummer: Enhetnummer, val navn: String) {

    class Enhetnummer(nummer: String) : Comparable<Enhetnummer> {

        @JsonValue
        val verdi = nummer.removePrefix(ENHET_PREFIX)

        init {
            requireDigits(verdi,4)
        }
        val gruppeNavn = "${ENHET_PREFIX}$verdi"

        override fun compareTo(other: Enhetnummer): Int = verdi.compareTo(other.verdi)

    }
    companion object {
        const val ENHET_PREFIX = "0000-GA-ENHET_"
    }
}

 class Tema(tema: String) : Comparable<Tema>   {

    @JsonValue
    val verdi = tema.removePrefix(TEMA_PREFIX)
    init {
        require(verdi.length == 3) { "Tema må være på tre bokstaver" }
        require(verdi.all { it.isLetter() }) { "Tema kan kun bestå av bokstaver" }
    }
     val gruppeNavn = "${TEMA_PREFIX}$verdi"

     override fun compareTo(other: Tema): Int = verdi.compareTo(other.verdi)

    companion object {
        const val TEMA_PREFIX = "0000-GA-TEMA_"
    }
}
package no.nav.sikkerhetstjenesten.entraproxy.graph

import com.fasterxml.jackson.annotation.JsonValue

class Tema(tema: String) : Comparable<Tema> {

   @JsonValue
   val verdi = tema.removePrefix(TEMA_PREFIX)

   init {
       require(verdi.length == 3) { "Tema må være på tre bokstaver" }
       require(verdi.all { it.isLetter() }) { "Tema kan kun bestå av bokstaver" }
   }

   val gruppeNavn = "${TEMA_PREFIX}$verdi"

   override fun compareTo(other: Tema): Int = verdi.compareTo(other.verdi)

   override fun equals(other: Any?): Boolean {
       if (this === other) return true
       if (other !is Tema) return false
       return verdi == other.verdi
   }

   override fun hashCode() = verdi.hashCode()

   companion object {
       const val TEMA_PREFIX = "0000-GA-TEMA_"
   }
}
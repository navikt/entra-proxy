package no.nav.sikkerhetstjenesten.entraproxy.graph

import com.fasterxml.jackson.annotation.JsonUnwrapped
import no.nav.sikkerhetstjenesten.entraproxy.graph.UtvidetAnsatt.Navn
import java.util.UUID

open class Ansatt(
                  val navIdent: AnsattId,
                  @param:JsonUnwrapped val navn: Navn): Comparable<Ansatt> {


    override fun compareTo(other: Ansatt): Int = navn.etternavn.compareTo(other.navn.etternavn)

}

class UtvidetAnsatt(navIdent: AnsattId, @JsonUnwrapped  navn: Navn, val tIdent: TIdent, val epost: String, val enhet: Enhet) : Ansatt(navIdent,navn) {
    data class Navn (val navn: String,val fornavn: String, val etternavn: String)
}

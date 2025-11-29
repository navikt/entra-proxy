package no.nav.sikkerhetstjenesten.entraproxy.graph

import no.nav.sikkerhetstjenesten.entraproxy.graph.UtvidetAnsatt.Navn

open class Ansatt(
                  val navIdent: AnsattId,
                  val navn: Navn): Comparable<Ansatt> {


    override fun compareTo(other: Ansatt): Int = navn.etternavn.compareTo(other.navn.etternavn)

}

class UtvidetAnsatt(navIdent: AnsattId,navn: Navn, val tIdent: TIdent, val epost: String, val enhet: Enhet) : Ansatt(navIdent,navn) {
    data class Navn (val navn: String,val fornavn: String, val etternavn: String)
}

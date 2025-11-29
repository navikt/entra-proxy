package no.nav.sikkerhetstjenesten.entraproxy.graph

import com.fasterxml.jackson.annotation.JsonUnwrapped
import no.nav.sikkerhetstjenesten.entraproxy.graph.UtvidetAnsatt.Navn

open class Ansatt(
                  val navIdent: AnsattId,
                  @param:JsonUnwrapped val navn: Navn): Comparable<Ansatt> {

    override fun compareTo(other: Ansatt): Int = navn.etternavn.compareTo(other.navn.etternavn)
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Ansatt) return false

        return navIdent == other.navIdent && navn == other.navn
    }
    override fun toString(): String {
        return "Ansatt(navIdent=$navIdent, navn=$navn)"
    }
}

class UtvidetAnsatt(navIdent: AnsattId, @JsonUnwrapped  navn: Navn, val tIdent: TIdent, val epost: String, val enhet: Enhet) : Ansatt(navIdent,navn) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is UtvidetAnsatt) return false
        if (!super.equals(other)) return false

        return tIdent == other.tIdent &&
                epost == other.epost &&
                enhet == other.enhet
    }
    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + tIdent.hashCode()
        result = 31 * result + epost.hashCode()
        result = 31 * result + enhet.hashCode()
        return result
    }
    override fun toString(): String {
        return "UtvidetAnsatt(navIdent=$navIdent, navn=$navn, tIdent=$tIdent, epost='$epost', enhet=$enhet)"
    }
    data class Navn (val navn: String,val fornavn: String, val etternavn: String)
}

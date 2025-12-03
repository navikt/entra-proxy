package no.nav.sikkerhetstjenesten.entraproxy.graph

import kotlin.hashCode


open class Ansatt(
                  val navIdent: AnsattId,
                  val visningNavn: String? = UKJENT, val fornavn: String? = UKJENT, val etternavn: String? = UKJENT): Comparable<Ansatt> {


    override fun compareTo(other: Ansatt): Int = navIdent.compareTo(other.navIdent)
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Ansatt) return false

        return navIdent == other.navIdent &&
                visningNavn == other.visningNavn &&
                fornavn == other.fornavn &&
                etternavn == other.etternavn
    }

    override fun hashCode(): Int {
        var result = navIdent.hashCode()
        result = 31 * result + (visningNavn?.hashCode() ?: 0)
        result = 31 * result + (fornavn?.hashCode() ?: 0)
        result = 31 * result + (etternavn?.hashCode() ?: 0)
        return result
    }

}

class UtvidetAnsatt(navIdent: AnsattId, visningNavn: String?,  fornavn: String?,  etternavn: String?, val tIdent: TIdent, val epost: String? = UKJENT, val enhet: Enhet) : Ansatt(navIdent,visningNavn,fornavn,etternavn) {
}

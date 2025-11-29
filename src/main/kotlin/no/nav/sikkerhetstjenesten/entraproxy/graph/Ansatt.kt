package no.nav.sikkerhetstjenesten.entraproxy.graph


open class Ansatt(
                  val navIdent: AnsattId,
                  val visningNavn: String, val fornavn: String, val etternavn: String): Comparable<Ansatt> {


    override fun compareTo(other: Ansatt): Int = etternavn.compareTo(other.etternavn)

}

class UtvidetAnsatt(navIdent: AnsattId, visningNavn: String,  fornavn: String,  etternavn: String, val tIdent: TIdent, val epost: String, val enhet: Enhet) : Ansatt(navIdent,visningNavn,fornavn,etternavn) {
}

package no.nav.sikkerhetstjenesten.entraproxy.graph


open class Ansatt(
                  val navIdent: AnsattId,
                  val visningNavn: String = UKJENT, val fornavn: String = UKJENT, val etternavn: String = UKJENT): Comparable<Ansatt> {


    override fun compareTo(other: Ansatt): Int = etternavn.compareTo(other.etternavn)

}

class UtvidetAnsatt(navIdent: AnsattId, visningNavn: String,  fornavn: String,  etternavn: String, val tIdent: TIdent, val epost: String, val enhet: Enhet) : Ansatt(navIdent,visningNavn,fornavn,etternavn) {
}

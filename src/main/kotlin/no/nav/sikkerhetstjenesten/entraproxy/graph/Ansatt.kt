package no.nav.sikkerhetstjenesten.entraproxy.graph

data class Ansatt(
    val navIdent: AnsattId,
    val visningNavn: String? = UKJENT,
    val fornavn: String? = UKJENT,
    val etternavn: String? = UKJENT) : Comparable<Ansatt> {

    override fun compareTo(other: Ansatt): Int = navIdent.compareTo(other.navIdent)
}

data class UtvidetAnsatt(
    val navIdent: AnsattId,
    val visningNavn: String? = UKJENT,
    val fornavn: String? = UKJENT,
    val etternavn: String? = UKJENT,
    val tIdent: TIdent,
    val epost: String? = UKJENT,
    val enhet: Enhet) : Comparable<UtvidetAnsatt> {

    override fun compareTo(other: UtvidetAnsatt): Int = navIdent.compareTo(other.navIdent)
}

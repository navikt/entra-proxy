package no.nav.sikkerhetstjenesten.entraproxy.graph

interface AnsattBasis {
    val navIdent: AnsattId
    val visningNavn: String?
    val fornavn: String?
    val etternavn: String?
}

data class Ansatt(
    override val navIdent: AnsattId,
    override val visningNavn: String? = UKJENT,
    override val fornavn: String? = UKJENT,
    override val etternavn: String? = UKJENT
) : AnsattBasis, Comparable<Ansatt> {

    override fun compareTo(other: Ansatt): Int = navIdent.compareTo(other.navIdent)
}

data class UtvidetAnsatt(
    override val navIdent: AnsattId,
    override val visningNavn: String? = UKJENT,
    override val fornavn: String? = UKJENT,
    override val etternavn: String? = UKJENT,
    val tIdent: TIdent,
    val epost: String? = UKJENT,
    val enhet: Enhet
) : AnsattBasis, Comparable<UtvidetAnsatt> {

    override fun compareTo(other: UtvidetAnsatt): Int = navIdent.compareTo(other.navIdent)
}

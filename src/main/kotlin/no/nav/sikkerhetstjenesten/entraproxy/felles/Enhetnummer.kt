package no.nav.sikkerhetstjenesten.entraproxy.felles

import com.fasterxml.jackson.annotation.JsonValue
import no.nav.sikkerhetstjenesten.entraproxy.graph.Enhet
import no.nav.sikkerhetstjenesten.felles.utils.extensions.DomainExtensions

data class Enhetnummer(private val nummer: String) : Comparable<Enhetnummer> {

    @JsonValue
    val verdi = nummer.removePrefix(Enhet.ENHET_PREFIX)

    init {
        DomainExtensions.requireDigits(verdi, 4)
    }
    val gruppeNavn = "${Enhet.ENHET_PREFIX}$verdi"

    override fun compareTo(other: Enhetnummer): Int = verdi.compareTo(other.verdi)

}
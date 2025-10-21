package no.nav.sikkerhetstjenesten.entraproxy.ansatt

import com.fasterxml.jackson.annotation.JsonValue
import no.nav.sikkerhetstjenesten.entraproxy.ansatt.graph.EntraGruppe
import no.nav.sikkerhetstjenesten.entraproxy.felles.utils.extensions.DomainExtensions.requireDigits


data class Ansatt(val ansattId: AnsattId, val grupper: Set<EntraGruppe>) {


    infix fun tilhører(enhet: Enhetsnummer?) =
        enhet?.let { e ->
            grupper.any { it.displayName.endsWith("ENHET_${e.verdi}") }
        } ?: false



    infix fun erMedlemAv(gruppe: GlobalGruppe) = grupper.any { it.id == gruppe.id
    }

    infix fun ikkeErMedlemAv(gruppe: GlobalGruppe) = !erMedlemAv(gruppe)


}
//flyttes til ansatt da Bruker ikke er i bruk her
data class Enhetsnummer(@JsonValue val verdi: String) {
    init {
        requireDigits(verdi, 4)
    }
}
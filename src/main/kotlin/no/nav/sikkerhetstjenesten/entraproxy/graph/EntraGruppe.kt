package no.nav.sikkerhetstjenesten.entraproxy.graph

import java.util.UUID

data class EntraGruppe(val rolle: String, val gruppeId: UUID? = null, val beskrivelse: String? = null) :
    Comparable<EntraGruppe> {
    override fun compareTo(other: EntraGruppe) = rolle.compareTo(other.rolle)
}

data class GruppeMedMedlemmer(val gruppeId: UUID, val gruppenavn: String, val medlemmer: Set<Ansatt>)

data class AnsattMedGrupper(val ansatt: Ansatt, val grupper: Set<String>)
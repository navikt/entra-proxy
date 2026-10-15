package no.nav.sikkerhetstjenesten.entraproxy.graph

import java.util.UUID

data class EntraGruppe(val rolle: String, val gruppeId: UUID? = null, val beskrivelse: String? = null): Comparable<EntraGruppe> {
    override fun compareTo(other: EntraGruppe): Int = rolle.compareTo(other.rolle)
}
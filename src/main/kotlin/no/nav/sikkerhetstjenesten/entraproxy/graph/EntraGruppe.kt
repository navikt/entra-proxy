package no.nav.sikkerhetstjenesten.entraproxy.graph

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.util.*

@JsonIgnoreProperties(ignoreUnknown = true)
data class EntraGruppe(val id: UUID? = null, val displayName: String = "N/A") {
    override fun equals(other: Any?) = other is EntraGruppe && id == other.id
    override fun hashCode() = id.hashCode()
}



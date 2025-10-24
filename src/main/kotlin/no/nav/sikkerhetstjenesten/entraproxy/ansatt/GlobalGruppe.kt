package no.nav.sikkerhetstjenesten.entraproxy.ansatt

import no.nav.sikkerhetstjenesten.entraproxy.ansatt.graph.EntraGruppe
import no.nav.sikkerhetstjenesten.entraproxy.tilgang.Token
import java.util.*

//behov for denne?

enum class GlobalGruppe(val property: String) {
    STRENGT_FORTROLIG("gruppe.strengt"),
    STRENGT_FORTROLIG_UTLAND("gruppe.strengt"),
    FORTROLIG("gruppe.fortrolig"),
    SKJERMING("gruppe.egenansatt"),
    UKJENT_BOSTED("gruppe.udefinert"),
    UTENLANDSK("gruppe.utland"),
    NASJONAL("gruppe.nasjonal");

    lateinit var id: UUID

    val entraGruppe get() = EntraGruppe(id)

    companion object {
        private fun navnFor(id: UUID) = entries.find { it.id == id }?.name ?: "Fant ikke gruppenavn for id $id"
        fun uuids() = entries.map { it.id }.toSet()
        fun setIDs(grupper: Map<String, UUID>) =
            entries.forEach { it.id = grupper[it.property] ?: error("Mangler id for ${it.property}") }

        fun Token.globaleGrupper() = globaleGruppeIds.intersect(uuids()).map { uuid ->
            EntraGruppe(uuid, navnFor(uuid))
        }.toSet()
        fun Set<EntraGruppe>.girNasjonalTilgang() = any { it.id == NASJONAL.id }
    }
}


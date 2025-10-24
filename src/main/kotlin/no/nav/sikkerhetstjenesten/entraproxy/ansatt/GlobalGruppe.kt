package no.nav.sikkerhetstjenesten.entraproxy.ansatt

import jakarta.annotation.PostConstruct
import no.nav.sikkerhetstjenesten.entraproxy.ansatt.graph.EntraGruppe
import no.nav.sikkerhetstjenesten.entraproxy.tilgang.Token
import org.springframework.boot.context.properties.ConfigurationProperties
import java.util.*

//behov for denne?

enum class GlobalGruppe(val property: String, val metadata: GruppeMetadata) {
    STRENGT_FORTROLIG("gruppe.strengt", GruppeMetadata.STRENGT_FORTROLIG),
    STRENGT_FORTROLIG_UTLAND("gruppe.strengt", GruppeMetadata.STRENGT_FORTROLIG_UTLAND),
    FORTROLIG("gruppe.fortrolig", GruppeMetadata.FORTROLIG),
    SKJERMING("gruppe.egenansatt", GruppeMetadata.SKJERMING),
    UKJENT_BOSTED("gruppe.udefinert", GruppeMetadata.UKJENT_BOSTED),
    UTENLANDSK("gruppe.utland", GruppeMetadata.UTENLANDSK),
    NASJONAL("gruppe.nasjonal", GruppeMetadata.NASJONAL);

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

@ConfigurationProperties("gruppe")
data class GlobaleGrupperConfig(val strengt: UUID, val nasjonal: UUID, val utland: UUID,
                                val udefinert: UUID, var fortrolig: UUID, val egenansatt: UUID) {

    @PostConstruct
    fun setIDs() {
        GlobalGruppe.setIDs(
            mapOf(
                "gruppe.strengt" to strengt,
                "gruppe.nasjonal" to nasjonal,
                "gruppe.utland" to utland,
                "gruppe.udefinert" to udefinert,
                "gruppe.fortrolig" to fortrolig,
                "gruppe.egenansatt" to egenansatt))
    }
}

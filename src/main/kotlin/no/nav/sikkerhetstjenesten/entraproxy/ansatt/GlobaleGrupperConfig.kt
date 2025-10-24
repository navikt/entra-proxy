package no.nav.sikkerhetstjenesten.entraproxy.ansatt

import jakarta.annotation.PostConstruct
import org.springframework.boot.context.properties.ConfigurationProperties
import java.util.UUID

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
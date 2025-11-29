package no.nav.sikkerhetstjenesten.entraproxy.graph

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import no.nav.sikkerhetstjenesten.entraproxy.graph.EntraSaksbehandlerRespons.AnsattRespons
import java.net.URI
import java.util.UUID

@JsonIgnoreProperties(ignoreUnknown = true)
internal data class Grupper(
    @param:JsonProperty("@odata.context") val next: URI? = null,
    val value: Set<IdentifiserbartObjekt> = emptySet()
)

@JsonIgnoreProperties(ignoreUnknown = true)
internal data class AnsattOids(
    @param:JsonProperty(VALUE) val oids: Set<AnsattOid>
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    internal data class AnsattOid(val id: UUID)
}

@JsonIgnoreProperties(ignoreUnknown = true)
internal data class Tilganger(
    @param:JsonProperty(NEXT_LINK) val next: URI? = null,
    val value: Set<IdentifiserbartObjekt> = emptySet()
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class EntraSaksbehandlerRespons(
    @param:JsonProperty(VALUE) val ansatte: Set<AnsattRespons>
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class AnsattRespons(
        val id: UUID,
        val onPremisesSamAccountName: String,
        val displayName: String = UKJENT,
        val givenName: String = UKJENT,
        val surname: String = UKJENT,
        val jobTitle: String = TIDENT_DEFAULT,
        val mail: String = UKJENT,
        val streetAddress: String = UKJENT_ENHET
    )
}

@JsonIgnoreProperties(ignoreUnknown = true)
internal data class IdentifiserbartObjekt(
    val id: UUID,
    val displayName: String = UKJENT
)

@JsonIgnoreProperties(ignoreUnknown = true)
internal data class GruppeMedlemmer(
    @param:JsonProperty(NEXT_LINK) val next: URI? = null,
    val value: Set<AnsattRespons> = emptySet()
)

internal const val TIDENT_DEFAULT = "AAA1234"
internal const val VALUE = "value"
internal const val UKJENT = "N/A"
internal  const val UKJENT_ENHET = "0000"
internal const val NEXT_LINK = "@odata.nextLink"


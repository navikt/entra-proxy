package no.nav.sikkerhetstjenesten.entraproxy.graph

import com.fasterxml.jackson.annotation.JsonAlias
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
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
internal data class EntraSaksbehandlerRespons(
    @param:JsonProperty(VALUE) val ansatte: Set<UtvidetAnsattRespons>
) {
    internal data class UtvidetAnsattRespons(
        val id: UUID,
        @param:JsonAlias(NAVIDENT) val navIdent: String,
        val displayName: String,
        val givenName: String,
        val surname: String,
        val jobTitle: String,
        val mail: String,
        val officeLocation: String
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
    val value: Set<GruppeMedlem> = emptySet()
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    internal data class GruppeMedlem(
        val id: UUID,
        @param:JsonProperty(NAVIDENT) val navIdent: String,
        val displayName: String = UKJENT,
        val givenName: String = UKJENT,
        val surname: String = UKJENT
    )
}

internal const val VALUE = "value"
internal const val UKJENT = "N/A"
internal const val NEXT_LINK = "@odata.nextLink"
internal const val NAVIDENT = "navIdent"


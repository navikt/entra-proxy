package no.nav.sikkerhetstjenesten.entraproxy.openapi

import io.swagger.v3.oas.models.OpenAPI
import org.springdoc.core.customizers.OpenApiCustomizer
import org.springframework.stereotype.Component

@Component
class OpenApiCustomiser : OpenApiCustomizer {
    override fun customise(openApi: OpenAPI) {
        val schemas = openApi.components?.schemas ?: return
        schemas["Ansatt"]?.apply {
            description = "Navn og ident for en ansatt"
            example = mapOf(
                "navIdent" to "A123456",
                "visningNavn" to "Tore Tang",
                "fornavn" to "Tore",
                "etternavn" to "Tang",
            )
        }
        schemas["Enhet"]?.apply {
            description = "Enhetnummer (4 siffer) og navn"
            example = mapOf(
                "enhetnummer" to "1234",
                "navn" to "Nav Avdeling Sydpolen",
            )
        }
    }
}

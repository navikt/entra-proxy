package no.nav.sikkerhetstjenesten.entraproxy.felles.openapi

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.media.Schema
import no.nav.sikkerhetstjenesten.entraproxy.graph.Ansatt
import no.nav.sikkerhetstjenesten.entraproxy.graph.AnsattId
import no.nav.sikkerhetstjenesten.entraproxy.graph.Enhet
import no.nav.sikkerhetstjenesten.entraproxy.graph.Enhet.Enhetnummer
import no.nav.sikkerhetstjenesten.entraproxy.graph.Tema
import org.springdoc.core.customizers.OpenApiCustomizer
import org.springframework.stereotype.Component

@Component
class OpenApiCustomiser : OpenApiCustomizer {
    override fun customise(openApi: OpenAPI) {
        val schemas = openApi.components.schemas
        schemas["Enhetnummer"] = Schema<Enhetnummer>().apply {
            type = "string"
            description = "Enhetnummer (4 siffer)"
            example = Enhetnummer("1234")
        }
        schemas["Enhet"] = Schema<Enhet>().apply {
            type = "object"
            description = "Enhetnummer (4 siffer) og navn"
            example = Enhet(Enhetnummer("1234"), "Nav Avdeling Sydpolen")
        }
        schemas["Ansatt"] = Schema<Ansatt>().apply {
            type = "string"
            description = "Navn og ident for en ansatt"
            example = Ansatt(AnsattId("A123456"), "Tore Tang", "Tore", "Tang")
        }
        schemas["NavIdent"] = Schema<Ansatt>().apply {
            type = "string"
            description = "NavIdent (7 siffer)"
            example = AnsattId("A123456")
        }
        schemas["Tema"] = Schema<Tema>().apply {
            type = "string"
            description = "Tema (3 store bokstaver)"
            example = Tema("AAP")
        }
    }
}

package no.nav.sikkerhetstjenesten.entraproxy.graph

import no.nav.sikkerhetstjenesten.entraproxy.graph.EntraGraphClient.Companion.GRAPH
import no.nav.sikkerhetstjenesten.entraproxy.graph.EntraGraphClient.Companion.TOP
import org.springframework.security.oauth2.client.annotation.ClientRegistrationId
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.service.annotation.GetExchange
import java.net.URI

@ClientRegistrationId(GRAPH)
interface EntraUserClient {

    @GetExchange("/users")
    fun users(
        @RequestParam($$"$select") select: String,
        @RequestParam($$"$filter") filter: String,
        @RequestParam($$"$count") count: String = "true",
    ): AnsattOids

    @GetExchange("/users")
    fun bruker(
        @RequestParam($$"$select") select: String,
        @RequestParam($$"$filter") filter: String,
        @RequestParam($$"$count") count: String = "true",
    ): EntraSaksbehandlerRespons

    @GetExchange("/users/{ansattId}/memberOf")
    fun memberOf(
        @PathVariable ansattId: String,
        @RequestParam($$"$select") select: String,
        @RequestParam($$"$filter", required = false) filter: String? = null,
        @RequestParam($$"$top") top: Int = TOP,
        @RequestParam($$"$count") count: String = "true",
    ): Tilganger

    @GetExchange("/users/{ansattId}/memberOf/microsoft.graph.group")
    fun memberOfGrupper(
        @PathVariable ansattId: String,
        @RequestParam($$"$select") select: String,
        @RequestParam($$"$filter", required = false) filter: String? = null,
        @RequestParam($$"$top") top: Int = TOP,
        @RequestParam($$"$count") count: String = "true",
    ): Tilganger

    @GetExchange
    fun tilgangerSide(uri: URI): Tilganger
}

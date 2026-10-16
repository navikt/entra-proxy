package no.nav.sikkerhetstjenesten.entraproxy.graph

import no.nav.sikkerhetstjenesten.entraproxy.graph.EntraGraphClient.Companion.GRAPH
import org.springframework.security.oauth2.client.annotation.ClientRegistrationId
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.service.annotation.GetExchange
import org.springframework.web.service.annotation.PostExchange
import java.net.URI

@ClientRegistrationId(GRAPH)
interface EntraGraphClient {

    @GetExchange("/users")
    fun users(
        @RequestParam($$"$select") select: String,
        @RequestParam($$"$filter") filter: String,
        @RequestParam($$"$count") count: String = "true"
    ): AnsattOids

    @GetExchange("/groups")
        fun groups(
            @RequestParam($$"$select") select: String,
        @RequestParam($$"$filter") filter: String,
        @RequestParam($$"$count") count: String = "true"
    ): Grupper

    @GetExchange("/users/{ansattId}/memberOf")
    fun memberOf(
        @PathVariable ansattId: String,
        @RequestParam($$"$select") select: String,
        @RequestParam($$"$filter", required = false) filter: String? = null,
        @RequestParam($$"$top") top: Int = TOP,
        @RequestParam($$"$count") count: String = "true"
    ): Tilganger

    @GetExchange("/users/{ansattId}/memberOf/microsoft.graph.group")
    fun memberOfGrupper(
        @PathVariable ansattId: String,
        @RequestParam($$"$select") select: String,
        @RequestParam($$"$filter", required = false) filter: String? = null,
        @RequestParam($$"$top") top: Int = TOP,
        @RequestParam($$"$count") count: String = "true"
    ): Tilganger

    @GetExchange("/groups/{gruppeId}/members")
    fun members(
        @PathVariable gruppeId: String,
        @RequestParam($$"$select") select: String,
        @RequestParam($$"$top") top: Int = TOP,
        @RequestParam($$"$count") count: String = "true"
    ): GruppeMedlemmer

    @PostExchange($$"/$batch")
    fun batch(@RequestBody request: BatchRequest): BatchRespons

    @PostExchange("/users/{avsender}/sendMail")
    fun sendMail(@PathVariable avsender: String, @RequestBody request: SendMailRequest)

    @GetExchange
    fun tilgangerSide(uri: URI): Tilganger

    @GetExchange
    fun gruppeMedlemmerSide(uri: URI): GruppeMedlemmer

    @GetExchange("/users")
    fun bruker(
        @RequestParam($$"$select") select: String,
        @RequestParam($$"$filter") filter: String,
        @RequestParam($$"$count") count: String = "true"
    ): EntraSaksbehandlerRespons

    @GetExchange(ENTRA_PING_PATH)
    fun ping(): Any?

    companion object {
        const val ENTRA_PING_PATH = "/organization"
        const val GRAPH = "graph"
        private const val TOP = 999
    }
}

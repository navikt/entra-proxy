package no.nav.sikkerhetstjenesten.entraproxy.graph

import no.nav.sikkerhetstjenesten.entraproxy.graph.EntraGraphClient.Companion.GRAPH
import org.springframework.security.oauth2.client.annotation.ClientRegistrationId
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.service.annotation.GetExchange
import org.springframework.web.service.annotation.HttpExchange
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

    @GetExchange("/groups/{gruppeId}/members")
    fun members(
        @PathVariable gruppeId: String,
        @RequestParam($$"$select") select: String,
        @RequestParam($$"$top") top: Int = TOP,
        @RequestParam($$"$count") count: String = "true"
    ): GruppeMedlemmer

    @HttpExchange(method = "GET", url = $$"/groups/{gruppeId}/members/$count", headers = ["ConsistencyLevel=eventual"])
    fun antallMedlemmer(@PathVariable gruppeId: String): String

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
        private const val TOP = 500
    }
}

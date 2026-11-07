package no.nav.sikkerhetstjenesten.entraproxy.graph

import no.nav.sikkerhetstjenesten.entraproxy.graph.EntraGraphClient.Companion.GRAPH
import no.nav.sikkerhetstjenesten.entraproxy.graph.EntraGraphClient.Companion.TOP
import org.springframework.security.oauth2.client.annotation.ClientRegistrationId
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.service.annotation.GetExchange
import java.net.URI

@ClientRegistrationId(GRAPH)
interface EntraGroupClient {

    @GetExchange("/groups")
    fun groups(
        @RequestParam($$"$select") select: String,
        @RequestParam($$"$filter") filter: String,
        @RequestParam($$"$count") count: String = "true",
    ): Grupper

    @GetExchange("/groups")
    fun grupperSok(
        @RequestParam($$"$select") select: String,
        @RequestParam($$"$search") search: String,
        @RequestParam($$"$top") top: Int = TOP,
        @RequestParam($$"$count") count: String = "true",
    ): Grupper

    @GetExchange("/groups/{gruppeId}/members")
    fun members(
        @PathVariable gruppeId: String,
        @RequestParam($$"$select") select: String,
        @RequestParam($$"$top") top: Int = TOP,
        @RequestParam($$"$count") count: String = "true",
    ): GruppeMedlemmer

    @GetExchange
    fun gruppeMedlemmerSide(uri: URI): GruppeMedlemmer

    @GetExchange
    fun grupperSide(uri: URI): Grupper
}

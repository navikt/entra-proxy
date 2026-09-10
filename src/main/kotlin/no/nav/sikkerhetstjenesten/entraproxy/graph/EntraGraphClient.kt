package no.nav.sikkerhetstjenesten.entraproxy.graph

import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.service.annotation.GetExchange
import org.springframework.web.service.annotation.HttpExchange

@HttpExchange
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
        @RequestParam($$"$top") top: Int = 250,
        @RequestParam($$"$count") count: String = "true"
    ): Tilganger

    @GetExchange("/groups/{gruppeId}/members")
    fun members(
        @PathVariable gruppeId: String,
        @RequestParam($$"$select") select: String,
        @RequestParam($$"$top") top: Int = 250,
        @RequestParam($$"$count") count: String = "true"
    ): GruppeMedlemmer

    @GetExchange("/users")
    fun bruker(
        @RequestParam($$"$select") select: String,
        @RequestParam($$"$filter") filter: String,
        @RequestParam($$"$count") count: String = "true"
    ): EntraSaksbehandlerRespons

    @GetExchange("/organization")
    fun ping(): Any?
}

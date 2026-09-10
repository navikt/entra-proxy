package no.nav.sikkerhetstjenesten.entraproxy.graph

import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.service.annotation.GetExchange
import org.springframework.web.service.annotation.HttpExchange

@HttpExchange
interface EntraGraphClient {

    @GetExchange("/users")
    fun users(
        @RequestParam("\$select") select: String,
        @RequestParam("\$count") count: String = "true",
        @RequestParam("\$filter") filter: String
    ): AnsattOids

    @GetExchange("/groups")
    fun groups(
        @RequestParam("\$select") select: String,
        @RequestParam("\$count") count: String = "true",
        @RequestParam("\$filter") filter: String
    ): Grupper

    @GetExchange("/users/{ansattId}/memberOf")
    fun memberOf(
        @PathVariable ansattId: String,
        @RequestParam("\$select") select: String,
        @RequestParam("\$count") count: String = "true",
        @RequestParam("\$top") top: Int = 250
    ): Tilganger

    @GetExchange("/groups/{gruppeId}/members")
    fun members(
        @PathVariable gruppeId: String,
        @RequestParam("\$select") select: String,
        @RequestParam("\$count") count: String = "true",
        @RequestParam("\$top") top: Int = 250
    ): GruppeMedlemmer

    @GetExchange("/users")
    fun usersByFilter(
        @RequestParam("\$select") select: String,
        @RequestParam("\$count") count: String = "true",
        @RequestParam("\$filter") filter: String
    ): EntraSaksbehandlerRespons

    @GetExchange("/organization")
    fun ping(): Any?
}

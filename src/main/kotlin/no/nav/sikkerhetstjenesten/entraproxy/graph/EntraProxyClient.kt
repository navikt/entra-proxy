package no.nav.sikkerhetstjenesten.entraproxy.graph

import no.nav.sikkerhetstjenesten.entraproxy.graph.EntraConfig.Companion.GRAPH
import org.springframework.security.oauth2.client.annotation.ClientRegistrationId
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.service.annotation.GetExchange
import org.springframework.web.service.annotation.HttpExchange

@HttpExchange
@ClientRegistrationId(GRAPH)
interface EntraProxyClient {

    @GetExchange(USERS_PATH)
    fun ansattOid(
        @RequestParam($$"$filter") filter: String,
        @RequestParam($$"$select") select: String = SELECT_USER,
        @RequestParam($$"$count") count: Boolean = true,
    ): AnsattOids

    @GetExchange(GROUPS_PATH)
    fun gruppeOid(
        @RequestParam($$"$filter") filter: String,
        @RequestParam($$"$select") select: String = TILGANG_EGENSKAPER,
        @RequestParam($$"$count") count: Boolean = true,
    ): Grupper

    @GetExchange(USER_MEMBER_OF_PATH)
    fun tema(
        @PathVariable oid: String,
        @RequestParam($$"$select") select: String = TILGANG_EGENSKAPER,
        @RequestParam($$"$count") count: Boolean = true,
        @RequestParam($$"$filter") filter: String = TEMA_QUERY,
        @RequestParam($$"$top") top: Int = DEFAULT_BATCH_SIZE,
    ): Tilganger

    @GetExchange(USER_MEMBER_OF_PATH)
    fun enheter(
        @PathVariable oid: String,
        @RequestParam($$"$select") select: String = TILGANG_EGENSKAPER,
        @RequestParam($$"$count") count: Boolean = true,
        @RequestParam($$"$filter") filter: String = ENHET_QUERY,
        @RequestParam($$"$top") top: Int = DEFAULT_BATCH_SIZE,
    ): Tilganger

    @GetExchange(USER_MEMBER_OF_PATH)
    fun ansatteGrupper(
        @PathVariable oid: String,
        @RequestParam($$"$select") select: String = TILGANG_EGENSKAPER,
        @RequestParam($$"$count") count: Boolean = true,
        @RequestParam($$"$filter") filter: String = SECENABLED,
        @RequestParam($$"$top") top: Int = DEFAULT_BATCH_SIZE,
    ): Tilganger

    @GetExchange(GROUP_MEMBERS_PATH)
    fun gruppeMedlemmer(
        @PathVariable gruppeId: String,
        @RequestParam($$"$select") select: String = ANSATT_EGENSKAPER,
        @RequestParam($$"$count") count: Boolean = true,
        @RequestParam($$"$top") top: Int = DEFAULT_BATCH_SIZE,
    ): GruppeMedlemmer

    @GetExchange(USERS_PATH)
    fun utvidetAnsatt(
        @RequestParam($$"$filter") filter: String,
        @RequestParam($$"$select") select: String = UTVIDET_ANSATT_EGENSKAPER,
    ): EntraSaksbehandlerRespons

    @GetExchange(PING_PATH)
    fun ping(): Any?

    companion object {
        private const val USERS_PATH = "/users"
        private const val GROUPS_PATH = "/groups"
        private const val USER_MEMBER_OF_PATH = "/users/{oid}/memberOf"
        private const val GROUP_MEMBERS_PATH = "/groups/{gruppeId}/members"
        private const val PING_PATH = "/organization"

        internal const val NAVIDENT = "onPremisesSamAccountName"
        internal const val T_IDENT = "jobTitle"
        private const val SELECT_USER = "id"
        private const val TILGANG_EGENSKAPER = "id,displayName"
        private const val ANSATT_EGENSKAPER = "id,givenName,surname,displayName,onPremisesSamAccountName"
        private const val UTVIDET_ANSATT_EGENSKAPER = "id,givenName,surname,displayName,mail,streetAddress,$T_IDENT,$NAVIDENT"
        private const val TEMA_QUERY = "startswith(displayName,'${Tema.TEMA_PREFIX}') "
        private const val ENHET_QUERY = "startswith(displayName,'${Enhet.ENHET_PREFIX}') "
        private const val SECENABLED = "securityEnabled eq true"
        private const val DEFAULT_BATCH_SIZE = 250
    }
}

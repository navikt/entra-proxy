package no.nav.sikkerhetstjenesten.entraproxy.graph

import no.nav.sikkerhetstjenesten.entraproxy.felles.cache.CachableConfig
import no.nav.sikkerhetstjenesten.entraproxy.graph.EntraConfig.Companion.GRAPH
import no.nav.sikkerhetstjenesten.entraproxy.felles.rest.AbstractRestConfig
import no.nav.sikkerhetstjenesten.entraproxy.felles.rest.CachableRestConfig
import no.nav.sikkerhetstjenesten.entraproxy.graph.Enhet.Companion.ENHET_PREFIX
import no.nav.sikkerhetstjenesten.entraproxy.graph.EntraOidCachableRestConfig.Companion.ENTRA_OID
import no.nav.sikkerhetstjenesten.entraproxy.graph.Tema.Companion.TEMA_PREFIX
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.web.util.UriBuilder
import java.net.URI
import java.time.Duration

@ConfigurationProperties(GRAPH)
class EntraConfig(
    baseUri: URI = DEFAULT_BASE_URI,
    pingPath: String = DEFAULT_PING_PATH,
    private val size: Int = DEFAULT_BATCH_SIZE,
    override val varighet : Duration,
    enabled: Boolean = true) : CachableRestConfig, AbstractRestConfig(baseUri, pingPath, GRAPH, enabled) {

    override val navn = name

    fun userURI(ansattId: String) =
        builder().apply {
            path(USERS_PATH)
            queryParams(this, SELECT_USER, "$NAVIDENT eq '$ansattId'")
        }.build()

    fun gruppeURI(displayName: String) =
        builder().apply {
            path(GRUPPER_PATH)
            queryParams(this, TILGANG_EGENSKAPER, "displayName eq '$displayName'")
        }.build()

    fun temaURI(oid: String) =
        grupperURI(oid, TEMA_QUERY)

    fun enheterURI(oid: String) =
        grupperURI(oid,ENHET_QUERY)

    fun gruppeMedlemmerURI(gruppeId: String) =
        builder().apply {
            path(MEDLEMMER_I_GRUPPE_PATH)
            queryParam(SELECT, ANSATT_EGENSKAPER)
            queryParam(COUNT, "true")
            queryParam(TOP, size)
        }.build(gruppeId)

    fun navIdentURI(ansattId: String) =
        identURI( "$NAVIDENT eq '$ansattId'")

    fun tIdentURI(ansattId: String) =
        identURI( "$T_IDENT eq '$ansattId'")

    fun ansatteGruppeURI(oid: String) =
        grupperURI(oid, SECENABLED)

    private fun identURI(filter: String) =
        builder().apply {
            path(USERS_PATH)
            queryParams(this, T_IDENT_NAVIDENT, filter)
        }.build()

    private fun grupperURI(oid: String, filter: String) =
        builder().apply {
            path(GRUPPER_FOR_ANSATT_PATH)
            queryParams(this, TILGANG_EGENSKAPER, filter)
            queryParam(TOP, size)
        }.build(oid)

    private fun queryParams(builder: UriBuilder, select: String, filter: String) =
        builder.apply {
            queryParam(SELECT, select)
            queryParam(COUNT, "true")
            queryParam(FILTER, filter)
        }

    override fun toString() = "$javaClass.simpleName [baseUri=$baseUri, pingEndpoint=$pingEndpoint]"

    companion object {
        const val GRAPH = "graph"
        const val NAVIDENT = "onPremisesSamAccountName"
        private val DEFAULT_BASE_URI = URI.create("https://graph.microsoft.com/v1.0")
        private const val T_IDENT = "jobTitle"
        private const val T_IDENT_NAVIDENT = "$T_IDENT,$NAVIDENT,id, givenName, surname,displayName,mail,streetAddress"
        private const val ANSATT_EGENSKAPER = "id, givenName, surname,displayName, $NAVIDENT"
        private const val TEMA_QUERY = "startswith(displayName,'$TEMA_PREFIX') "
        private const val ENHET_QUERY = "startswith(displayName,'${ENHET_PREFIX}') "
        private const val SECENABLED = "securityEnabled eq true"
        private const val DEFAULT_BATCH_SIZE = 250
        private const val USERS_PATH = "/users"
        private const val GRUPPER_PATH = "/groups"
        private const val GRUPPER_FOR_ANSATT_PATH = "/users/{ansattId}/memberOf"
        private const val MEDLEMMER_I_GRUPPE_PATH = "/groups/{gruppeId}/members"
        private const val SELECT = "\$select"
        private const val FILTER = "\$filter"
        private const val COUNT = "\$count"
        private const val SELECT_USER = "id"
        private const val TILGANG_EGENSKAPER = "id,displayName"
        private const val DEFAULT_PING_PATH = "/organization"
        private const val TOP = "\$top"
        val OID_CACHE = CachableConfig(ENTRA_OID)
    }
}
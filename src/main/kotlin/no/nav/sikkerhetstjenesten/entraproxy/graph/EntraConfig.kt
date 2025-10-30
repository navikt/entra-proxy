package no.nav.sikkerhetstjenesten.entraproxy.graph

import no.nav.sikkerhetstjenesten.entraproxy.graph.EntraConfig.Companion.GRAPH
import no.nav.sikkerhetstjenesten.entraproxy.felles.rest.AbstractRestConfig
import no.nav.sikkerhetstjenesten.entraproxy.felles.rest.CachableRestConfig
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.web.util.UriBuilder
import java.net.URI
import java.time.Duration

@ConfigurationProperties(GRAPH)
class EntraConfig(
    baseUri: URI = DEFAULT_BASE_URI,
    pingPath: String = DEFAULT_PING_PATH,
    private val size: Int = DEFAULT_BATCH_SIZE,
    enabled: Boolean = true) : CachableRestConfig, AbstractRestConfig(baseUri, pingPath, GRAPH, enabled) {

    override val navn = name
    override val varighet = Duration.ofHours(3)

    fun userURI(ansattId: String) =
        builder().apply {
            path(USERS_PATH)
            queryParams(this, SELECT_USER, "$KONTO eq '$ansattId'")
        }.build()

    fun gruppeURI(displayName: String) =
        builder().apply {
            path(GRUPPER_PATH)
            queryParams(this, GRUPPE_PROPERTIES, "displayName eq '$displayName'")
        }.build()

    fun temaURI(oid: String) =
        grupperURI(oid, TEMA_QUERY)

    fun enheterURI(oid: String) =
        grupperURI(oid,ENHET_QUERY)

    fun medlemmerGrupperURI(gruppeId: String) =
        builder().apply {
            path(MEDLEMMER_I_GRUPPE_PATH)
            queryParams(this,KONTO , "")
            queryParam(TOP, size)
        }.build(gruppeId)


    private fun grupperURI(oid: String, filter: String) =
        builder().apply {
            path(GRUPPER_FOR_ANSATT_PATH)
            queryParams(this, GRUPPE_PROPERTIES, filter)
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
        const val TEMA_PREFIX = "0000-GA-TEMA_"
        const val ENHET_PREFIX = "0000-GA-ENHET_"
        private val DEFAULT_BASE_URI = URI.create("https://graph.microsoft.com/v1.0")
        private const val KONTO = "onPremisesSamAccountName"
        private const val TEMA_QUERY = "startswith(displayName,'$TEMA_PREFIX') "
        private const val ENHET_QUERY = "startswith(displayName,'$ENHET_PREFIX') "
        private const val DEFAULT_BATCH_SIZE = 250
        private const val USERS_PATH = "/users"
        private const val GRUPPER_PATH = "/groups"
        private const val GRUPPER_FOR_ANSATT_PATH = "/users/{ansattId}/memberOf"
        private const val MEDLEMMER_I_GRUPPE_PATH = "/groups/{gruppeId}/members"
        private const val SELECT = "\$select"
        private const val FILTER = "\$filter"
        private const val COUNT = "\$count"
        private const val SELECT_USER = "id"
        private const val GRUPPE_PROPERTIES = "id,displayName"
        private const val DEFAULT_PING_PATH = "/organization"
        private const val TOP = "\$top"
    }
}
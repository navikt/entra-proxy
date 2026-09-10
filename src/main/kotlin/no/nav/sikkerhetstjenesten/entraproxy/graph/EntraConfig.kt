package no.nav.sikkerhetstjenesten.entraproxy.graph

import no.nav.sikkerhetstjenesten.entraproxy.felles.cache.CachableConfig
import no.nav.sikkerhetstjenesten.entraproxy.graph.EntraConfig.Companion.GRAPH
import no.nav.sikkerhetstjenesten.entraproxy.felles.rest.AbstractRestConfig
import no.nav.sikkerhetstjenesten.entraproxy.felles.rest.CachableRestConfig
import no.nav.sikkerhetstjenesten.entraproxy.graph.EntraOidCachableRestConfig.Companion.ENTRA_OID
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.web.util.UriBuilder
import java.net.URI
import java.time.Duration

@ConfigurationProperties(GRAPH)
class EntraConfig(
    baseUri: URI = DEFAULT_BASE_URI,
    pingPath: String = DEFAULT_PING_PATH,
    override val varighet : Duration) : CachableRestConfig, AbstractRestConfig(baseUri, pingPath, GRAPH) {

    override val navn = name

    fun userURI(ansattId: String) =
        builder().apply {
            path(USERS_PATH)
            queryParams(this, SELECT_USER, "$NAVIDENT eq '$ansattId'")
        }.build()

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
        private const val USERS_PATH = "/users"
        private const val SELECT = "\$select"
        private const val FILTER = "\$filter"
        private const val COUNT = "\$count"
        private const val SELECT_USER = "id"
        private const val DEFAULT_PING_PATH = "/organization"
        val OID_CACHE = CachableConfig(ENTRA_OID)
    }
}
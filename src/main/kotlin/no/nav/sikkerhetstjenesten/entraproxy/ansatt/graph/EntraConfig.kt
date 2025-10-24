package no.nav.sikkerhetstjenesten.entraproxy.ansatt.graph

import no.nav.sikkerhetstjenesten.entraproxy.ansatt.GlobalGruppe
import no.nav.sikkerhetstjenesten.entraproxy.ansatt.graph.EntraConfig.Companion.GRAPH
import no.nav.sikkerhetstjenesten.entraproxy.felles.rest.AbstractRestConfig
import no.nav.sikkerhetstjenesten.entraproxy.felles.rest.CachableRestConfig
import org.springframework.boot.context.properties.ConfigurationProperties
import java.net.URI
import java.time.Duration

@ConfigurationProperties(GRAPH)
class EntraConfig(
    baseUri: URI,
    pingPath: String = DEFAULT_PING_PATH,
    private val size: Int = DEFAULT_BATCH_SIZE,
    enabled: Boolean = true) : CachableRestConfig, AbstractRestConfig(baseUri, pingPath, GRAPH, enabled) {

    override val navn = name
    override val varighet = Duration.ofHours(3)

    fun userURI(navIdent: String) = builder().apply {
        path(USERS_PATH)
        queryParam(PARAM_NAME_SELECT, PARAM_VALUE_SELECT_USER)
        queryParam(PARAM_NAME_FILTER, "onPremisesSamAccountName eq '$navIdent'")
        queryParam(PARAM_NAME_COUNT, "true")
    }.build()

    fun temaURI(oid: String) = ccTemaUri(oid)

    fun grupperURI(ansattId: String, isCCF: Boolean) = if (isCCF) ccUri(ansattId) else oboUri(ansattId)

    private fun ccTemaUri(oid: String) = query(oid,TEMA_PREFIX)

    private fun oboUri(ansattId: String) = query(ansattId,GEO_PREFIX)

    private fun ccUri(ansattId: String) = query(ansattId,"id in(${uuidsFormatted()}) or $GEO_PREFIX")

    private fun query(ansattId: String, filter: String) = builder().apply {
        path(GRUPPER_PATH)
        queryParam(PARAM_NAME_SELECT, PARAM_VALUE_SELECT_GROUPS)
        queryParam(PARAM_NAME_COUNT, "true")
        queryParam(PARAM_NAME_TOP, size)
        queryParam(PARAM_NAME_FILTER, filter)
    }.build(ansattId)

    private fun uuidsFormatted() = GlobalGruppe.uuids().joinToString(separator ="','" , prefix = "'", postfix = "'")

    override fun toString() = "$javaClass.simpleName [baseUri=$baseUri, pingEndpoint=$pingEndpoint]"

    companion object {
        const val TEMA_PREFIX = "startswith(displayName,'0000-GA-TEMA') "
        const val GEO_PREFIX = "startswith(displayName,'0000-GA-GEO') or startswith(displayName,'0000-GA-ENHET') "
        const val GRAPH = "graph"
        private const val DEFAULT_BATCH_SIZE = 250
        private const val USERS_PATH = "/users"
        private const val GRUPPER_PATH = "/users/{ansattId}/memberOf"
        private const val PARAM_NAME_SELECT = "\$select"
        private const val PARAM_NAME_FILTER = "\$filter"
        private const val PARAM_NAME_COUNT = "\$count"
        private const val PARAM_VALUE_SELECT_USER = "id"
        private const val PARAM_VALUE_SELECT_GROUPS = "id,displayName"
        private const val DEFAULT_PING_PATH = "/organization"
        private const val PARAM_NAME_TOP = "\$top"
    }
}
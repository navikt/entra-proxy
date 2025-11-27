package no.nav.sikkerhetstjenesten.entraproxy.graph

import no.nav.sikkerhetstjenesten.entraproxy.felles.rest.AbstractRestConfig
import no.nav.sikkerhetstjenesten.entraproxy.felles.rest.CachableRestConfig
import no.nav.sikkerhetstjenesten.entraproxy.graph.BetaEntraConfig.Companion.GRAPHBETA
import no.nav.sikkerhetstjenesten.entraproxy.graph.Enhet.Companion.ENHET_PREFIX
import no.nav.sikkerhetstjenesten.entraproxy.graph.Tema.Companion.TEMA_PREFIX
import org.springframework.boot.context.properties.ConfigurationProperties
import java.net.URI
import java.time.Duration

@ConfigurationProperties(GRAPHBETA)
class BetaEntraConfig(
    baseUri: URI = DEFAULT_BASE_URI,
    pingPath: String = DEFAULT_PING_PATH,
    private val size: Int = DEFAULT_BATCH_SIZE,
    enabled: Boolean = true) : CachableRestConfig, AbstractRestConfig(baseUri, pingPath, GRAPHBETA, enabled) {

    override val navn = name
    override val varighet = Duration.ofHours(3)

    override fun toString() = "$javaClass.simpleName [baseUri=$baseUri, pingEndpoint=$pingEndpoint]"

    companion object {
        const val GRAPHBETA = "graph"
        const val NAVIDENT = "onPremisesSamAccountName"
        private val DEFAULT_BASE_URI = URI.create("https://graph.microsoft.com/beta")
        private const val T_IDENT = "jobTitle"
        private const val T_IDENT_NAVIDENT = "$T_IDENT,$NAVIDENT,id, givenName, surname,displayName,mail,officeLocation "
        private const val ANSATT_EGENSKAPER = "id, givenName, surname,displayName, $NAVIDENT"
        private const val TEMA_QUERY = "startswith(displayName,'$TEMA_PREFIX') "
        private const val ENHET_QUERY = "startswith(displayName,'$ENHET_PREFIX') "
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
    }
}
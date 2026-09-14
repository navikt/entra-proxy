package no.nav.sikkerhetstjenesten.entraproxy.tilgang

import no.nav.security.mock.oauth2.MockOAuth2Server
import no.nav.sikkerhetstjenesten.entraproxy.felles.rest.AuthContext.Companion.NAVIDENT
import no.nav.sikkerhetstjenesten.entraproxy.felles.rest.AuthContext.Companion.OID
import no.nav.sikkerhetstjenesten.entraproxy.felles.rest.AuthContext.Companion.ROLES
import no.nav.sikkerhetstjenesten.entraproxy.felles.utils.cluster.ClusterConstants.NAIS_CLUSTER_NAME
import no.nav.sikkerhetstjenesten.entraproxy.graph.AnsattId
import no.nav.sikkerhetstjenesten.entraproxy.graph.Enhet
import no.nav.sikkerhetstjenesten.entraproxy.graph.Enhet.Enhetnummer
import no.nav.sikkerhetstjenesten.entraproxy.security.CLIENT_CREDENTIALS
import no.nav.sikkerhetstjenesten.entraproxy.tilgang.SecurityTestSupport.SecurityTestOAuth2.server
import org.springframework.test.context.DynamicPropertyRegistry
import java.util.UUID

object SecurityTestSupport {
    val TEST_ANSATT_ID = AnsattId("Z123456")
    val TEST_ENHET = Enhet(Enhetnummer("1234"), "Testenhet")
    const val TEST_ISSUER_ID = "azuread"
    const val TEST_SUBJECT = "subject"
    const val TEST_AUDIENCE = "test-audience"
    const val ISSUER_URI_PROPERTY = "spring.security.oauth2.resourceserver.jwt.issuer-uri"
    const val AUDIENCES_PROPERTY = "spring.security.oauth2.resourceserver.jwt.audiences"

    object SecurityTestOAuth2 {
        val server = MockOAuth2Server().also { it.start() }

        init {
            Runtime.getRuntime().addShutdownHook(Thread {
                server.shutdown()
            })
        }
    }

    fun OBOjwt(aud: String = TEST_AUDIENCE, ansattId: AnsattId = TEST_ANSATT_ID, claims: Map<String,Any> = emptyMap()) = server.issueToken(
        TEST_ISSUER_ID, TEST_SUBJECT, aud,
        mapOf(NAVIDENT to ansattId.verdi, OID to "${UUID.randomUUID()}") + claims,
    ).serialize()

    fun CCjwt(aud: String = TEST_AUDIENCE, ansattId: AnsattId = TEST_ANSATT_ID, claims: Map<String,Any> = emptyMap()) = server.issueToken(
        TEST_ISSUER_ID, TEST_SUBJECT, aud,
        mapOf(NAVIDENT to ansattId.verdi, ROLES to listOf(CLIENT_CREDENTIALS)) + claims,
    ).serialize()

    fun DynamicPropertyRegistry.setProperties(clusterName: String? = null) {
        val issuerUrl = server.issuerUrl(TEST_ISSUER_ID).toString()
        add(ISSUER_URI_PROPERTY) { issuerUrl }
        add(AUDIENCES_PROPERTY) { TEST_AUDIENCE }
        clusterName?.let { add(NAIS_CLUSTER_NAME) { it } }
    }


}
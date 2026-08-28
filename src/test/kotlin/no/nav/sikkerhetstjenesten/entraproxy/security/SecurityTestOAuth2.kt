package no.nav.sikkerhetstjenesten.entraproxy.security

import no.nav.security.mock.oauth2.MockOAuth2Server
import no.nav.sikkerhetstjenesten.entraproxy.felles.LogbookBeanConfiguration
import no.nav.sikkerhetstjenesten.entraproxy.felles.cache.CacheTestConfig
import no.nav.sikkerhetstjenesten.entraproxy.felles.cache.CaffeineCacheOperations
import no.nav.sikkerhetstjenesten.entraproxy.felles.rest.Token.Companion.NAVIDENT
import no.nav.sikkerhetstjenesten.entraproxy.felles.rest.Token.Companion.OID
import no.nav.sikkerhetstjenesten.entraproxy.felles.utils.cluster.ClusterConstants.NAIS_CLUSTER_NAME
import no.nav.sikkerhetstjenesten.entraproxy.graph.AnsattId
import no.nav.sikkerhetstjenesten.entraproxy.norg.NorgConfig.Companion.NORG
import no.nav.sikkerhetstjenesten.entraproxy.security.SecurityTestOAuth2.server
import no.nav.sikkerhetstjenesten.entraproxy.tilgang.EntraController
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.FilterType
import org.springframework.context.annotation.FilterType.ASSIGNABLE_TYPE
import org.springframework.context.annotation.Import
import org.springframework.test.context.DynamicPropertyRegistry
import tools.jackson.databind.json.JsonMapper
import java.util.UUID

internal val TEST_ANSATT_ID = AnsattId("Z999999")
internal const val TEST_ISSUER_ID = "azuread"
internal const val TEST_SUBJECT = "subject"
internal const val TEST_AUDIENCE = "test-audience"
internal const val INVALID_AUDIENCE = "invalid-audience"
internal const val ISSUER_URI_PROPERTY = "spring.security.oauth2.resourceserver.jwt.issuer-uri"
internal const val AUDIENCES_PROPERTY = "spring.security.oauth2.resourceserver.jwt.audiences"

internal object SecurityTestOAuth2 {
    val server = MockOAuth2Server().also { it.start() }
}

internal fun jwt(aud: String, ansattId: AnsattId, claims: Map<String,Any> = emptyMap()) = server.issueToken(
    TEST_ISSUER_ID, TEST_SUBJECT, aud,
    mapOf(NAVIDENT to ansattId.verdi, OID to "${UUID.randomUUID()}") + claims,
).serialize()

internal fun DynamicPropertyRegistry.setProperties(clusterName: String? = null) {
    add(ISSUER_URI_PROPERTY, server.issuerUrl(TEST_ISSUER_ID)::toString)
    add(AUDIENCES_PROPERTY, TEST_AUDIENCE::toString)
    clusterName?.let { add(NAIS_CLUSTER_NAME) { it } }
}

@TestConfiguration
class NorgTestConfig : CacheTestConfig(NORG)

@SpringBootApplication
@ComponentScan(excludeFilters = [ComponentScan.Filter(type = ASSIGNABLE_TYPE, classes = [OAuth2ClientBeanConfig::class])])
@Import(
    OAuth2SecurityBeanConfig::class,
    EntraController::class,
    NorgTestConfig::class,
    JsonMapper::class,
    LogbookBeanConfiguration::class,
    CaffeineCacheOperations::class
)
class SecurityTestApplication
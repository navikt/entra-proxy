package no.nav.entraproxy.graph

import no.nav.sikkerhetstjenesten.entraproxy.graph.EntraConfig
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class EntraConfigTest {

    @Test
    fun `medlemmerURI returnerer forventet URI`() {
        val config = EntraConfig()
        val gruppeId = "fd2cfaab-9f62-4d6f-9c44-93766154c98a"
        val uri = config.medlemmerURI(gruppeId)

        val forventaBaseUrl = "https://graph.microsoft.com/v1.0/groups/$gruppeId/members"
        println(uri.toString())
        assertEquals(true, uri.toString().startsWith(forventaBaseUrl))
        assertEquals(true, uri.query?.contains("\$top=250") == true)
        assertEquals(true, uri.query?.contains("\$select=id,onPremisesSamAccountName") == true)
    }
}
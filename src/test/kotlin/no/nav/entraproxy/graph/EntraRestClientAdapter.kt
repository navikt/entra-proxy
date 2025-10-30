package no.nav.entraproxy.graph

import io.mockk.every
import io.mockk.mockk
import no.nav.sikkerhetstjenesten.entraproxy.graph.EntraConfig
import no.nav.sikkerhetstjenesten.entraproxy.graph.EntraRestClientAdapter
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.net.URI
import java.util.*

class EntraRestClientAdapterTest {

    private val restClient = mockk<org.springframework.web.client.RestClient>()
    private val config = mockk<EntraConfig>()
    private val adapter = EntraRestClientAdapter(restClient, config)

    @Test
    fun `medlemmer returnerer forventet sett med brukernavn`() {
        val oid = UUID.randomUUID().toString()
        val uri = URI("http://test")
        val ansatt1 = EntraRestClientAdapter.EntraAnsattRespons.EntraAnsattData(UUID.randomUUID(), "A123456")
        val ansatt2 = EntraRestClientAdapter.EntraAnsattRespons.EntraAnsattData(UUID.randomUUID(), "B678900")
        val respons =  setOf(ansatt1, ansatt2)

        every { config.medlemmerURI(oid) } returns uri
        every { adapter.medlemmer(oid)} returns respons.map { it.onPremisesSamAccountName!! }.toSet()

        val result = adapter.medlemmer(oid)
        println(result)
        assertEquals(setOf("A123456", "B678900"), result)
    }
}
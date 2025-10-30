package no.nav.entraproxy.graph

import io.mockk.every
import io.mockk.mockk
import no.nav.sikkerhetstjenesten.entraproxy.graph.AnsattId
import no.nav.sikkerhetstjenesten.entraproxy.graph.Enhet
import no.nav.sikkerhetstjenesten.entraproxy.graph.EntraRestClientAdapter
import no.nav.sikkerhetstjenesten.entraproxy.graph.EntraTjeneste
import no.nav.sikkerhetstjenesten.entraproxy.graph.Tema
import no.nav.sikkerhetstjenesten.entraproxy.norg.NorgTjeneste
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.*

class EntraTjenesteTest {

    private val adapter = mockk<EntraRestClientAdapter>()
    private val norgTjeneste = mockk<NorgTjeneste>()
    private val entraTjeneste = EntraTjeneste(adapter, norgTjeneste)

    @Test
    fun `enhetMedlemmer returnerer forventet sett med AnsattId`() {
        val enhetnummer = Enhet.Enhetnummer("1234")
        val gruppeId = UUID.randomUUID()
        val navIdenter = setOf("A123456", "B654321")

        every { adapter.medlemmer(gruppeId.toString()) } returns navIdenter

        val result = entraTjeneste.enhetMedlemmer(enhetnummer, gruppeId)
        println(result)
        assertEquals(navIdenter.map { AnsattId(it) }.toSet(), result)
    }

    @Test
    fun `temaMedlemmer returnerer forventet sett med AnsattId`() {
        val tema = Tema("ABC")
        val gruppeId = UUID.randomUUID()
        val navIdenter = setOf("C111111", "D222222")

        every { adapter.medlemmer(gruppeId.toString()) } returns navIdenter

        val result = entraTjeneste.temaMedlemmer(tema, gruppeId)
        println(result)
        assertEquals(navIdenter.map { AnsattId(it) }.toSet(), result)
    }
}
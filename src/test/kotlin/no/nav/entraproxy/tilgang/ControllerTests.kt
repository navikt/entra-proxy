package no.nav.entraproxy.tilgang
import io.mockk.every
import io.mockk.mockk
import no.nav.sikkerhetstjenesten.entraproxy.graph.AnsattOidTjeneste
import no.nav.sikkerhetstjenesten.entraproxy.graph.Enhet
import no.nav.sikkerhetstjenesten.entraproxy.graph.EntraTjeneste
import no.nav.sikkerhetstjenesten.entraproxy.tilgang.DevEntraController
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class DevEntraControllerTest {

    private val entraTjeneste = mockk<EntraTjeneste>()
    private val oidTjeneste = mockk<AnsattOidTjeneste>()
    private val controller = DevEntraController(entraTjeneste, oidTjeneste)

    @Test
    fun `enhetMedlemmer returns expected result when groupId exists`() {
        val enhetsnummer = Enhet.Enhetnummer("1234")
        val gruppeId = java.util.UUID.randomUUID()
        val expectedResult = setOf(
            no.nav.sikkerhetstjenesten.entraproxy.graph.AnsattId("A123456"),
            no.nav.sikkerhetstjenesten.entraproxy.graph.AnsattId("B678900")
        )

        every { entraTjeneste.gruppeIdForEnhet(enhetsnummer) } returns gruppeId
        every { entraTjeneste.enhetMedlemmer(enhetsnummer, gruppeId) } returns expectedResult

        val result = controller.enhetMedlemmer(enhetsnummer)
        assertEquals(expectedResult, result)
    }

    @Test
    fun `enhetMedlemmer returns null when groupId is null`() {
        val enhetsnummer = Enhet.Enhetnummer("1234")
        every { entraTjeneste.gruppeIdForEnhet(enhetsnummer) } returns null

        val result = controller.enhetMedlemmer(enhetsnummer)
        assertEquals(null, result)
    }
}
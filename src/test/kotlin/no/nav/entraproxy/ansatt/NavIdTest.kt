package no.nav.entraproxy.ansatt


import no.nav.sikkerhetstjenesten.entraproxy.graph.AnsattId
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class NavIdTest {
    @Test
    @DisplayName("Gyldig ansattId OK")
    fun validNavId() {
        assertEquals("A123456", AnsattId("A123456").verdi)
    }

    @Test
    @DisplayName("ansattId med ugyldig lengde skal kaste IllegalArgumentException")
    fun lengde() {
        assertThrows<IllegalArgumentException> { AnsattId("A12345") }
    }

    @Test
    @DisplayName("ansattId uten bokstav først skal kaste IllegalArgumentException")
    fun ikkeBokstav() {
        assertThrows<IllegalArgumentException> { AnsattId("&123456") }
    }

    @Test
    @DisplayName("ansattId uten 6 tall etter første bokstav skal kaste IllegalArgumentException")
    fun ikke6tall() {
        assertThrows<IllegalArgumentException> { AnsattId("A12345a") }
    }
}
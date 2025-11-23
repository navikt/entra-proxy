import tools.jackson.module.kotlin.readValue
import org.junit.jupiter.api.Test
import no.nav.sikkerhetstjenesten.entraproxy.graph.Ansatt
import no.nav.sikkerhetstjenesten.entraproxy.graph.AnsattUtvidetInfo
import tools.jackson.module.kotlin.jacksonObjectMapper

class AnsattSerializationTest {
    private val objectMapper = jacksonObjectMapper()

    @Test
    fun `Ansatt serialiseres og deserialiseres korrekt`() {
        val json = """
            {
              "onPremisesSamAccountName": "B654321",
              "displayName": "Kari Nordmann",
              "givenName": "Kari",
              "surname": "Nordmann"
            }
        """.trimIndent()
        val deserialized = objectMapper.readValue<Ansatt>(json)
        println(deserialized)
    }
    @Test
    fun `AnsattUtvidetInfo serialiseres og deserialiseres korrekt`() {
        val json = """
            {
              "onPremisesSamAccountName": "B654321",
              "displayName": "Kari Nordmann",
              "givenName": "Kari",
              "surname": "Nordmann",
              "jobTitle": "T123456",
              "mail": "kari@nav.no",
              "officeLocation": "Oslo"
            }
        """.trimIndent()
        val deserialized = objectMapper.readValue<AnsattUtvidetInfo>(json)
        println(deserialized)
    }
}


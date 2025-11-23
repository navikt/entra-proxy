import tools.jackson.module.kotlin.readValue
import org.junit.jupiter.api.Test
import no.nav.sikkerhetstjenesten.entraproxy.graph.AnsattUtvidetInfo
import tools.jackson.module.kotlin.jacksonObjectMapper

class AnsattSerializationTest {
    private val objectMapper = jacksonObjectMapper()

    @Test
    fun `AnsattUtvidetInfo serialiseres og deserialiseres korrekt`() {
        val json = """
            {
              "id": "7a9cca1f-8552-4042-ad1a-255efb740991",
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


package no.nav.entraproxy.graph

import com.fasterxml.jackson.annotation.JsonUnwrapped
import no.nav.sikkerhetstjenesten.entraproxy.felles.cache.CacheNavPolymorphicTypeValidator
import no.nav.sikkerhetstjenesten.entraproxy.felles.cache.CacheTypeInfoAddingJacksonModule
import no.nav.sikkerhetstjenesten.entraproxy.graph.Ansatt
import no.nav.sikkerhetstjenesten.entraproxy.graph.AnsattId
import no.nav.sikkerhetstjenesten.entraproxy.graph.Enhet
import no.nav.sikkerhetstjenesten.entraproxy.graph.Enhet.Companion.ENHET_PREFIX
import no.nav.sikkerhetstjenesten.entraproxy.graph.Enhet.Enhetnummer
import no.nav.sikkerhetstjenesten.entraproxy.graph.TIdent
import no.nav.sikkerhetstjenesten.entraproxy.graph.Tema
import no.nav.sikkerhetstjenesten.entraproxy.graph.Tema.Companion.TEMA_PREFIX
import no.nav.sikkerhetstjenesten.entraproxy.graph.UtvidetAnsatt
import org.assertj.core.api.Assertions.assertThat
import tools.jackson.databind.SerializationFeature.FAIL_ON_UNWRAPPED_TYPE_IDENTIFIERS
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.KotlinModule.Builder
import tools.jackson.module.kotlin.readValue
import kotlin.test.Test


class TilgangTester {

    private val mapper = JsonMapper.builder().polymorphicTypeValidator(CacheNavPolymorphicTypeValidator()).apply {
        disable(FAIL_ON_UNWRAPPED_TYPE_IDENTIFIERS)
        addModule(Builder().build())
        addModule(CacheTypeInfoAddingJacksonModule())
    }.build()
    private val nummer = "1234"
    private val aap = "AAP"
    val enhet = Enhetnummer(nummer)
    val enhet1 = Enhetnummer("${ENHET_PREFIX}$nummer")

    val tema = Tema(aap)
    val tema1 = Tema("${TEMA_PREFIX}$aap")

    @Test
    fun enhetUtenPrefix() {
        assertThat(enhet.gruppeNavn).isEqualTo("${ENHET_PREFIX}$nummer")
        assertThat(enhet.verdi).isEqualTo(nummer)
    }
    @Test
    fun enhetMedPrefix() {
        assertThat(enhet1.gruppeNavn).isEqualTo("${ENHET_PREFIX}$nummer")
        assertThat(enhet1.verdi).isEqualTo(nummer)
    }

    @Test
    fun serDeserEnhet() {
        assertThat( mapper.readValue<Enhetnummer>(mapper.writeValueAsString(enhet)).verdi).isEqualTo(nummer)
        assertThat( mapper.readValue<Enhetnummer>(mapper.writeValueAsString(enhet1)).verdi).isEqualTo(nummer)
    }
    @Test
    fun temaUtenPrefix() {
        assertThat(tema.gruppeNavn).isEqualTo("${TEMA_PREFIX}$aap")
        assertThat(tema.verdi).isEqualTo(aap)
    }
    @Test
    fun temaMedPrefix() {
        assertThat(tema1.gruppeNavn).isEqualTo("${TEMA_PREFIX}$aap")
        assertThat(tema1.verdi).isEqualTo(aap)
    }

    @Test
    fun serDeserTema() {
        assertThat( mapper.readValue<Tema>(mapper.writeValueAsString(tema)).verdi).isEqualTo(aap)
        assertThat( mapper.readValue<Tema>(mapper.writeValueAsString(tema1)).verdi).isEqualTo(aap)
    }
    @Test
    fun test1() {
        val a  = AnsattId("A123456")
        val c = UtvidetAnsatt(AnsattId("A123456"), UtvidetAnsatt.Navn("Ola Nordmann", "Ola", "Nordmann"),
            TIdent("AAA1234"), "epost", Enhet(Enhetnummer("1234"), "Navn"))
        val s = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(c)
        println("mapper " + s)
        val c1 = mapper.readValue<UtvidetAnsatt>(s)
        //println(c1)
        //assertEquals(a, c1)
    }
}


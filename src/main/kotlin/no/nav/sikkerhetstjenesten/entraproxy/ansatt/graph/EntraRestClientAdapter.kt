package no.nav.sikkerhetstjenesten.entraproxy.ansatt.graph

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import no.nav.sikkerhetstjenesten.entraproxy.ansatt.Enhetsnummer
import no.nav.sikkerhetstjenesten.entraproxy.ansatt.Tema
import no.nav.sikkerhetstjenesten.entraproxy.ansatt.graph.EntraConfig.Companion.GRAPH
import no.nav.sikkerhetstjenesten.entraproxy.felles.rest.AbstractRestClientAdapter
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import no.nav.sikkerhetstjenesten.entraproxy.ansatt.graph.EntraConfig.Companion.ENHET_PREFIX
import no.nav.sikkerhetstjenesten.entraproxy.ansatt.graph.EntraConfig.Companion.TEMA_PREFIX
import java.net.URI
import java.util.*

@Component
class EntraRestClientAdapter(@Qualifier(GRAPH) restClient: RestClient, val cf: EntraConfig) :
    AbstractRestClientAdapter(restClient, cf) {

    fun oidFraEntra(ansattId: String) =
        get<EntraAnsattRespons>(cf.userURI(ansattId)).oids.single().id

    fun tema(oid: String) = buildSet {
        generateSequence(get<EntraGrupper>(cf.temaURI(oid))) { bolk ->
            bolk.next?.let { get<EntraGrupper>(it) }
        }.forEach { addAll(it.value) }
    }//.map { Tema(it.displayName.substringAfter(TEMA_PREFIX)) }.toSet()


    fun enheter(ansattId: String) = buildSet {
        generateSequence(get<EntraGrupper>(cf.enheterURI(ansattId))) { bolk -> bolk.next?.let { get<EntraGrupper>(it) }
        }.forEach { addAll(it.value) }
    }.map {
        Enhetsnummer(it.displayName.substringAfter(ENHET_PREFIX))
    }.toSet()


    @JsonIgnoreProperties(ignoreUnknown = true)
    private data class EntraAnsattRespons(@param:JsonProperty("value") val oids: Set<EntraOids>) {
            data class EntraOids(val id: UUID)
    }

    override fun toString() = "${javaClass.simpleName} [client=$restClient, config=$cf, errorHandler=$errorHandler]"

    @JsonIgnoreProperties(ignoreUnknown = true)
    private data class EntraGrupper(@param:JsonProperty("@odata.nextLink") val next: URI? = null,
                                    val value: Set<EntraGruppe> = emptySet())
}


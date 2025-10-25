package no.nav.sikkerhetstjenesten.entraproxy.ansatt

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import no.nav.sikkerhetstjenesten.entraproxy.felles.rest.AbstractRestClientAdapter
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import java.net.URI
import java.util.*

@Component
class EntraRestClientAdapter(@Qualifier(EntraConfig.Companion.GRAPH) restClient: RestClient, val cf: EntraConfig) :
    AbstractRestClientAdapter(restClient, cf) {

    fun oidFraEntra(ansattId: String) =
        get<EntraAnsattRespons>(cf.userURI(ansattId)).oids.single().id

    fun tema(oid: String) =
        buildSet {
            generateSequence(get<EntraGrupper>(cf.temaURI(oid))) { bolk ->
                bolk.next?.let { get<EntraGrupper>(it) }
            }.flatMap { it.value }
                .forEach { add(Tema(it.displayName.substringAfter(EntraConfig.Companion.TEMA_PREFIX))) }
        }

    fun enheter(oid: String) =
        buildSet {
            generateSequence(get<EntraGrupper>(cf.enheterURI(oid))) { bolk ->
                bolk.next?.let { get<EntraGrupper>(it) }
            }.flatMap { it.value }
                .forEach { add(Enhetsnummer(it.displayName.substringAfter(EntraConfig.Companion.ENHET_PREFIX))) }
        }


    @JsonIgnoreProperties(ignoreUnknown = true)
    private data class EntraAnsattRespons(@param:JsonProperty("value") val oids: Set<EntraOids>) {
            data class EntraOids(val id: UUID)
    }

    override fun toString() = "${javaClass.simpleName} [client=$restClient, config=$cf, errorHandler=$errorHandler]"

    @JsonIgnoreProperties(ignoreUnknown = true)
    private data class EntraGrupper(@param:JsonProperty("@odata.nextLink") val next: URI? = null,
                                    val value: Set<EntraGruppe> = emptySet())
}


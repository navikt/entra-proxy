package no.nav.sikkerhetstjenesten.entraproxy.ansatt.graph

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import no.nav.sikkerhetstjenesten.entraproxy.ansatt.graph.EntraConfig.Companion.GRAPH
import no.nav.sikkerhetstjenesten.entraproxy.felles.rest.AbstractRestClientAdapter
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import java.util.*

@Component
class EntraRestClientAdapter(@Qualifier(GRAPH) restClient: RestClient, val cf: EntraConfig) :
    AbstractRestClientAdapter(restClient, cf) {

    fun oidFraEntra(ansattId: String) =
        get<EntraSaksbehandlerRespons>(cf.userURI(ansattId)).oids.single().id

    fun tema(oid: String): Set<EntraGruppe> =
        generateSequence(get<EntraGrupper>(cf.temaURI(oid))) { bolk ->
            bolk.next?.let {
                get<EntraGrupper>(it)
            }
        }
            .flatMap { it.value }
            .toSet()

    fun grupper(ansattId: String, trengerGlobaleGrupper: Boolean): Set<EntraGruppe> =
        generateSequence(get<EntraGrupper>(cf.grupperURI(ansattId,trengerGlobaleGrupper))) { bolk ->
            bolk.next?.let {
                get<EntraGrupper>(it)
            }
        }
            .flatMap { it.value }
            .toSet()


    @JsonIgnoreProperties(ignoreUnknown = true)
    data class EntraSaksbehandlerRespons(@param:JsonProperty("value") val oids: Set<EntraOids>) {
        data class EntraOids(val id: UUID)
    }

    override fun toString() = "${javaClass.simpleName} [client=$restClient, config=$cf, errorHandler=$errorHandler]"

}
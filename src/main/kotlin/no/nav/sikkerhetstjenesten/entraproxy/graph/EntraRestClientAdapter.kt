package no.nav.sikkerhetstjenesten.entraproxy.graph

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import no.nav.sikkerhetstjenesten.entraproxy.felles.rest.AbstractRestClientAdapter
import no.nav.sikkerhetstjenesten.entraproxy.graph.Enhet.Enhetnummer
import no.nav.sikkerhetstjenesten.entraproxy.graph.EntraConfig.Companion.ENHET_PREFIX
import no.nav.sikkerhetstjenesten.entraproxy.graph.EntraConfig.Companion.GRAPH
import no.nav.sikkerhetstjenesten.entraproxy.graph.EntraConfig.Companion.TEMA_PREFIX
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import java.net.URI
import java.util.*

@Component
class EntraRestClientAdapter(@Qualifier(GRAPH) restClient: RestClient, val cf: EntraConfig) :
    AbstractRestClientAdapter(restClient, cf) {

    fun oidFraEntra(ansattId: String) =
        get<EntraAnsattRespons>(cf.userURI(ansattId)).oids.single().id

    fun tema(oid: String) =
        buildSet {
            generateSequence(get<EntraGrupper>(cf.temaURI(oid))) { it.next?.let(::get) }
                .flatMap { it.value }
                .forEach { add(Tema(it.displayName.removePrefix(TEMA_PREFIX))) }
        }

    fun enheter(oid: String) =
        buildSet {
            generateSequence(get<EntraGrupper>(cf.enheterURI(oid))) { it.next?.let(::get) }
                .flatMap { it.value }
                .forEach { add(Enhetnummer(it.displayName.removePrefix(ENHET_PREFIX))) }
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


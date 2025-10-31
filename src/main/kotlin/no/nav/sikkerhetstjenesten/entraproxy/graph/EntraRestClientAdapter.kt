package no.nav.sikkerhetstjenesten.entraproxy.graph

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import no.nav.sikkerhetstjenesten.entraproxy.felles.rest.AbstractRestClientAdapter
import no.nav.sikkerhetstjenesten.entraproxy.graph.Enhet.Enhetnummer
import no.nav.sikkerhetstjenesten.entraproxy.graph.EntraConfig.Companion.ENHET_PREFIX
import no.nav.sikkerhetstjenesten.entraproxy.graph.EntraConfig.Companion.GRAPH
import no.nav.sikkerhetstjenesten.entraproxy.graph.EntraConfig.Companion.TEMA_PREFIX
import no.nav.sikkerhetstjenesten.entraproxy.graph.EntraRestClientAdapter.EntraGrupper.EntraGruppe
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import java.net.URI
import java.util.*

@Component
class EntraRestClientAdapter(@Qualifier(GRAPH) restClient: RestClient, val cf: EntraConfig) :
    AbstractRestClientAdapter(restClient, cf) {

    fun oid(ansattId: String) =
        get<EntraAnsattOidRespons>(cf.userURI(ansattId)).oids.single().id

    fun gruppeId(displayName: String) =
        get<EntraGruppeRespons>(cf.gruppeURI(displayName)).value.firstOrNull()?.id

    fun tema(oid: String) =
        grupper(cf.temaURI(oid), TEMA_PREFIX, ::Tema)

    fun enheter(oid: String) =
        grupper(cf.enheterURI(oid), ENHET_PREFIX, ::Enhetnummer)

    fun medlemmer(oid: String)  =
        buildSet {
            generateSequence(get<EntraAnsatteRespons>(cf.medlemmerURI(oid))) { it.next?.let(::get) }
                .flatMap { it.value }
                .forEach { add(AnsattId(it.navIdent)) }
        }

    private inline fun <T> grupper(uri: URI, prefix: String, crossinline constructorOn: (String) -> T) =
        buildSet {
            generateSequence(get<EntraGrupper>(uri)) { it.next?.let(::get) }
                .flatMap { it.value }
                .forEach { add(constructorOn(it.displayName.removePrefix(prefix))) }
        }

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class EntraAnsatteRespons(@param:JsonProperty("@odata.nextLink") val next: URI? = null, val value: Set<EntraMedlemmerAnsatt> = emptySet())


    @JsonIgnoreProperties(ignoreUnknown = true)
    data class EntraMedlemmerAnsatt(@param:JsonProperty("onPremisesSamAccountName")  val navIdent: String)

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class EntraGruppeRespons(@param:JsonProperty("@odata.context") val next: URI? = null, val value: Set<EntraGruppe> = emptySet())

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class EntraAnsattOidRespons(@param:JsonProperty("value") val oids: Set<EntraAnsattData>) {
        data class EntraAnsattData(val id: UUID, @param:JsonProperty("onPremisesSamAccountName") val navIdent: String? = null)
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class EntraGrupper(@param:JsonProperty("@odata.nextLink") val next: URI? = null,
                                    val value: Set<EntraGruppe> = emptySet()) {
        @JsonIgnoreProperties(ignoreUnknown = true)
        data class EntraGruppe(val id: UUID, val displayName: String)
    }
    override fun toString() = "${javaClass.simpleName} [client=$restClient, config=$cf, errorHandler=$errorHandler]"
}


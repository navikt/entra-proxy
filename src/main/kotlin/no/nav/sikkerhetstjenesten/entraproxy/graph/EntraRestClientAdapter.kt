package no.nav.sikkerhetstjenesten.entraproxy.graph

import io.opentelemetry.api.trace.Span
import no.nav.sikkerhetstjenesten.entraproxy.felles.rest.NotFoundRestException
import no.nav.sikkerhetstjenesten.entraproxy.felles.rest.Pingable
import no.nav.sikkerhetstjenesten.entraproxy.graph.Enhet.Enhetnummer
import no.nav.sikkerhetstjenesten.entraproxy.graph.EntraConfig.Companion.GRAPH
import no.nav.sikkerhetstjenesten.entraproxy.graph.EntraSaksbehandlerRespons.AnsattRespons
import no.nav.sikkerhetstjenesten.entraproxy.norg.NorgTjeneste
import org.slf4j.LoggerFactory.getLogger
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.http.HttpStatus.NOT_FOUND
import org.springframework.stereotype.Component
import org.springframework.web.ErrorResponseException
import org.springframework.web.client.RestClient
import org.springframework.web.client.body
import org.springframework.web.service.registry.ImportHttpServices
import java.net.URI

@Component
@ImportHttpServices(group = GRAPH, types = [EntraProxyClient::class])
class EntraRestClientAdapter(
    private val restClient: RestClient,
    private val client: EntraProxyClient,
    val cf: EntraConfig,
    private val norg: NorgTjeneste) : Pingable {

    private val log = getLogger(javaClass)

    override fun ping() = client.ping()
    override val name = cf.name
    override val pingEndpoint = cf.pingEndpoint

    val baseURI = cf.baseUri

    fun ansattOid(navIdent: String) =
        with(client.ansattOid("$NAVIDENT eq '$navIdent'").oids) {
            log.info("Fant $size oids ($this) i Entra for $navIdent")
            when (size) {
                0 -> throw NotFoundRestException(cf.userURI(navIdent), msg = "Fant ingen oid for navident $navIdent, er den fremdeles gyldig?")
                1 -> singleOrNull()?.id
                else -> throw EntraOidException(navIdent, "Forventet nøyaktig én oid for navident $navIdent, fant $size (${joinToString(", ") { it.id.toString() }})")
            }
        }

    fun gruppeOid(gruppeNavn: String) =
        client.gruppeOid("displayName eq '$gruppeNavn'").value.firstOrNull()?.id

    fun tema(ansattOid: String): Set<Tema> =
        run {
            val resultat = mutableSetOf<Tema>()
            var side = client.tema(ansattOid)
            while (true) {
                side.value.forEach { resultat.add(Tema(it.displayName)) }
                val nesteSide = side.next ?: break
                side = get(nesteSide)
            }
            resultat.toSortedSet()
        }

    fun enheter(ansattOid: String): Set<Enhetnummer> =
        run {
            val resultat = mutableSetOf<Enhetnummer>()
            var side = client.enheter(ansattOid)
            while (true) {
                side.value.forEach { resultat.add(Enhetnummer(it.displayName)) }
                val nesteSide = side.next ?: break
                side = get(nesteSide)
            }
            resultat.toSortedSet()
        }

    fun ansatteGrupper(ansattOid: String): Set<EntraGruppe> =
        run {
            val resultat = mutableSetOf<EntraGruppe>()
            var side = client.ansatteGrupper(ansattOid)
            while (true) {
                side.value.forEach { resultat.add(EntraGruppe(it.displayName)) }
                val nesteSide = side.next ?: break
                side = get(nesteSide)
            }
            resultat.toSortedSet()
        }

    fun gruppeMedlemmer(gruppeOid: String): Set<Ansatt> =
        run {
            val resultat = mutableSetOf<Ansatt>()
            var side = client.gruppeMedlemmer(gruppeOid)
            while (true) {
                side.value.forEach {
                    resultat.add(Ansatt(AnsattId(it.onPremisesSamAccountName), it.displayName, it.givenName, it.surname))
                }
                val nesteSide = side.next ?: break
                side = get(nesteSide)
            }
            resultat.toSortedSet()
        }

    fun utvidetAnsatt(ansattId: String) =
        ansatt {
            client.utvidetAnsattNavIdent("$NAVIDENT eq '$ansattId'").ansatte.firstOrNull()
        }

    fun utvidetAnsattTident(ansattId: String) =
        ansatt {
            client.utvidetAnsattTIdent("$T_IDENT eq '$ansattId'").ansatte.firstOrNull()
        }

    private inline fun <reified T : Any> get(uri: URI) =
        restClient.get()
            .uri(uri)
            .accept(APPLICATION_JSON)
            .retrieve()
            .body<T>() ?: throw NotFoundRestException(uri, msg = "Fant tomt svar fra nextLink")

    private fun ansatt(block: () -> AnsattRespons?) =
        block()?.let {
            with(it) {
                val enhetsNummer = Enhetnummer(streetAddress ?: UKJENT_ENHET)
                UtvidetAnsatt(
                    AnsattId(onPremisesSamAccountName), displayName, givenName, surname,
                    TIdent(jobTitle ?: TIDENT_DEFAULT),
                    mail,
                    Enhet(enhetsNummer, norg.navnFor(enhetsNummer)),
                )
            }
        }

    override fun toString() =
        "${javaClass.simpleName} [client=$client, config=$cf]"

    private companion object {
        const val NAVIDENT = "onPremisesSamAccountName"
        const val T_IDENT = "jobTitle"
    }
}

class EntraOidException(ansattId: String, msg: String) : ErrorResponseException(NOT_FOUND) {
    init {
        body.title = TITLE
        body.detail = msg
        body.properties = mapOf("navIdent" to ansattId, "traceId" to Span.current().spanContext.traceId)
    }

    companion object {
        const val TITLE = "Uventet respons fra Entra"
    }
}

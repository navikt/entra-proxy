package no.nav.sikkerhetstjenesten.entraproxy.graph

import io.opentelemetry.api.trace.Span
import no.nav.sikkerhetstjenesten.entraproxy.felles.rest.NotFoundRestException
import no.nav.sikkerhetstjenesten.entraproxy.felles.rest.Pingable
import no.nav.sikkerhetstjenesten.entraproxy.graph.Enhet.Enhetnummer
import no.nav.sikkerhetstjenesten.entraproxy.graph.EntraConfig.Companion.GRAPH
import no.nav.sikkerhetstjenesten.entraproxy.graph.EntraSaksbehandlerRespons.AnsattRespons
import no.nav.sikkerhetstjenesten.entraproxy.norg.NorgTjeneste
import org.slf4j.LoggerFactory.getLogger
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
        buildSet {
            generateSequence(client.tema(ansattOid)) { it.next?.let(::get) }
                .forEach { side -> side.value.forEach { add(Tema(it.displayName)) } }
        }.toSortedSet()

    fun enheter(ansattOid: String): Set<Enhetnummer> =
        buildSet {
            generateSequence(client.enheter(ansattOid)) { it.next?.let(::get) }
                .forEach { side -> side.value.forEach { add(Enhetnummer(it.displayName)) } }
        }.toSortedSet()

    fun ansatteGrupper(ansattOid: String): Set<EntraGruppe> =
        buildSet {
            generateSequence(client.ansatteGrupper(ansattOid)) { it.next?.let(::get) }
                .forEach { side -> side.value.forEach { add(EntraGruppe(it.displayName)) } }
        }.toSortedSet()

    fun gruppeMedlemmer(gruppeOid: String): Set<Ansatt> =
        buildSet {
            generateSequence(client.gruppeMedlemmer(gruppeOid)) { it.next?.let(::get) }
                .forEach { side ->
                    side.value.forEach {
                    add(Ansatt(AnsattId(it.onPremisesSamAccountName), it.displayName, it.givenName, it.surname))
                    }
                }
        }.toSortedSet()

    fun utvidetAnsatt(ansattId: String) =
        hentUtvidetAnsatt("$NAVIDENT eq '$ansattId'")

    fun utvidetAnsattTident(ansattId: String) =
        hentUtvidetAnsatt("$T_IDENT eq '$ansattId'")

    private inline fun <reified T : Any> get(uri: URI) =
        restClient.get()
            .uri(uri)
            .accept(APPLICATION_JSON)
            .retrieve()
            .body<T>() ?: throw NotFoundRestException(uri, "Fant tomt svar fra nextLink")

    private fun hentUtvidetAnsatt(filter: String) =
        ansatt {
            client.utvidetAnsatt(filter).ansatte.firstOrNull()
        }

    private fun ansatt(block: () -> AnsattRespons?) =
        block()?.let {
            with(it) {
                val enhetsNummer = Enhetnummer(streetAddress)
                UtvidetAnsatt(
                    AnsattId(onPremisesSamAccountName), displayName, givenName, surname,
                    TIdent(jobTitle),
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
        body.title = "Uventet respons fra Entra"
        body.detail = msg
        body.properties = mapOf("navIdent" to ansattId, "traceId" to Span.current().spanContext.traceId)
    }
}

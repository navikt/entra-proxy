package no.nav.sikkerhetstjenesten.entraproxy.graph

import io.opentelemetry.api.trace.Span
import no.nav.sikkerhetstjenesten.entraproxy.felles.rest.DefaultRestErrorHandler
import no.nav.sikkerhetstjenesten.entraproxy.felles.rest.IrrecoverableRestException
import no.nav.sikkerhetstjenesten.entraproxy.felles.rest.NotFoundRestException
import no.nav.sikkerhetstjenesten.entraproxy.felles.rest.Pingable
import no.nav.sikkerhetstjenesten.entraproxy.graph.Enhet.Enhetnummer
import no.nav.sikkerhetstjenesten.entraproxy.graph.EntraConfig.Companion.GRAPH
import org.slf4j.LoggerFactory.getLogger
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR
import org.springframework.http.HttpStatus.NOT_FOUND
import org.springframework.http.HttpStatusCode
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.stereotype.Component
import org.springframework.web.ErrorResponseException
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClient.ResponseSpec.ErrorHandler
import org.springframework.web.client.body
import java.net.URI

@Component
class EntraRestClientAdapter(
    @param:Qualifier(GRAPH) val restClient: RestClient,
    val cf: EntraConfig,
    val errorHandler: ErrorHandler = DefaultRestErrorHandler()) : Pingable {

    val log = getLogger(javaClass)

    override fun ping() = get<Any>(cf.pingEndpoint)
    override val name = cf.name
    override val pingEndpoint = "${cf.pingEndpoint}"

    val baseURI = cf.baseUri

    fun ansattOid(navIdent: String) =
        with(get<AnsattOids>(cf.userURI(navIdent)).oids) {
            log.info("Fant $size oids ($this) i Entra for $navIdent")
            when (size) {
                0 -> throw NotFoundRestException(cf.userURI(navIdent), msg = "Fant ingen oid for navident $navIdent, er den fremdeles gyldig?")
                1 -> singleOrNull()?.id
                else -> throw EntraOidException(navIdent, "Forventet nøyaktig én oid for navident $navIdent, fant $size (${joinToString(", ") { it.id.toString() }})")
            }
        }

    fun gruppeOid(gruppeNavn: String) =
        get<Grupper>(cf.gruppeURI(gruppeNavn)).value.firstOrNull()?.id

    fun tema(ansattOid: String): Set<Tema> =
        generateSequence(get<Tilganger>(cf.temaURI(ansattOid))) { page ->
            page.next?.let { uri -> get<Tilganger>(uri) }
        }
            .flatMap { it.value }
            .map { Tema(it.displayName) }
            .toSortedSet()

    fun enheter(ansattOid: String): Set<Enhetnummer> =
        generateSequence(get<Tilganger>(cf.enheterURI(ansattOid))) { page ->
            page.next?.let {
                uri -> get<Tilganger>(uri)
            }
        }.flatMap { it.value }
            .map { Enhetnummer(it.displayName) }
            .toSortedSet()

    fun ansatteGrupper(ansattOid: String): Set<EntraGruppe> =
        generateSequence(get<Tilganger>(cf.ansatteGruppeURI(ansattOid))) { page ->
            page.next?.let { uri -> get<Tilganger>(uri) }
        }.flatMap { it.value }
            .map { EntraGruppe(it.displayName) }
            .toSortedSet()

    fun gruppeMedlemmer(gruppeOid: String): Set<Ansatt> =
        generateSequence(get<GruppeMedlemmer>(cf.gruppeMedlemmerURI(gruppeOid))) { page ->
            page.next?.let { uri -> get<GruppeMedlemmer>(uri) }
        }.flatMap { it.value }
            .map { medlem ->
                Ansatt(
                    AnsattId(medlem.onPremisesSamAccountName),
                    medlem.displayName,
                    medlem.givenName,
                    medlem.surname
                )
            }
            .toSortedSet()

    fun utvidetAnsatt(ansattId: String) =
        utvidetAnsatt(cf.navIdentURI(ansattId), ansattId)

    fun utvidetAnsattTident(ansattId: String) =
        utvidetAnsatt(cf.tIdentURI(ansattId), ansattId)

    private fun utvidetAnsatt(uri: URI, ident: String) =
        get<EntraSaksbehandlerRespons>(uri).ansatte.firstOrNull()

    final inline fun <reified T : Any> get(uri: URI, headers: Map<String, String> = emptyMap()) =
        restClient.get()
            .uri(uri)
            .accept(APPLICATION_JSON)
            .headers { it.setAll(headers) }
            .retrieve()
            .onStatus(HttpStatusCode::isError, errorHandler::handle)
            .body<T>() ?: throw IrrecoverableRestException(INTERNAL_SERVER_ERROR, uri)

    override fun toString() = "${javaClass.simpleName} [client=$restClient, config=$cf, errorHandler=$errorHandler]"
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

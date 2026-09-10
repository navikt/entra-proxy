package no.nav.sikkerhetstjenesten.entraproxy.graph

import io.opentelemetry.api.trace.Span
import no.nav.sikkerhetstjenesten.entraproxy.felles.rest.NotFoundRestException
import no.nav.sikkerhetstjenesten.entraproxy.felles.rest.Pingable
import no.nav.sikkerhetstjenesten.entraproxy.graph.Enhet.Enhetnummer
import org.slf4j.LoggerFactory.getLogger
import org.springframework.http.HttpStatus.NOT_FOUND
import org.springframework.stereotype.Component
import org.springframework.web.ErrorResponseException

@Component
class EntraRestClientAdapter(private val graphClient: EntraGraphClient, val cf: EntraConfig) : Pingable {

    val log = getLogger(javaClass)

    override fun ping() = graphClient.ping()
    override val name = cf.name
    override val pingEndpoint = "${cf.pingEndpoint}"

    val baseURI = cf.baseUri

    fun ansattOid(navIdent: String) =
        with(graphClient.users(
            select = "id",
            filter = "onPremisesSamAccountName eq '$navIdent'"
        ).oids) {
            log.info("Fant $size oids ($this) i Entra for $navIdent")
            when (size) {
                0 -> throw NotFoundRestException(cf.userURI(navIdent), msg = "Fant ingen oid for navident $navIdent, er den fremdeles gyldig?")
                1 -> singleOrNull()?.id
                else -> throw EntraOidException(navIdent, "Forventet nøyaktig én oid for navident $navIdent, fant $size (${joinToString(", ") { it.id.toString() }})")
            }
        }

    fun gruppeOid(gruppeNavn: String) =
        graphClient.groups(
            select = "id,displayName",
            filter = "displayName eq '$gruppeNavn'"
        ).value.firstOrNull()?.id

    fun tema(ansattOid: String): Set<Tema> =
        graphClient.memberOf(ansattOid, select = "id,displayName")
            .value
            .map { Tema(it.displayName) }
            .toSortedSet()

    fun enheter(ansattOid: String): Set<Enhetnummer> =
        graphClient.memberOf(ansattOid, select = "id,displayName")
            .value
            .map { Enhetnummer(it.displayName) }
            .toSortedSet()

    fun ansatteGrupper(ansattOid: String): Set<EntraGruppe> =
        graphClient.memberOf(ansattOid, select = "id,displayName")
            .value
            .map { EntraGruppe(it.displayName) }
            .toSortedSet()

    fun gruppeMedlemmer(gruppeOid: String): Set<Ansatt> =
        graphClient.members(gruppeOid, select = "id, givenName, surname,displayName, onPremisesSamAccountName")
            .value
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
        graphClient.usersByFilter(
            select = "jobTitle,onPremisesSamAccountName,id,givenName,surname,displayName,mail,streetAddress",
            filter = "onPremisesSamAccountName eq '$ansattId'"
        ).ansatte.firstOrNull()

    fun utvidetAnsattTident(ansattId: String) =
        graphClient.usersByFilter(
            select = "jobTitle,onPremisesSamAccountName,id,givenName,surname,displayName,mail,streetAddress",
            filter = "jobTitle eq '$ansattId'"
        ).ansatte.firstOrNull()

    override fun toString() = "${javaClass.simpleName} [client=$graphClient, config=$cf]"
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

package no.nav.sikkerhetstjenesten.entraproxy.graph

import io.opentelemetry.api.trace.Span
import no.nav.sikkerhetstjenesten.entraproxy.felles.rest.NotFoundRestException
import no.nav.sikkerhetstjenesten.entraproxy.felles.rest.Pingable
import no.nav.sikkerhetstjenesten.entraproxy.graph.Enhet.Companion.ENHET_PREFIX
import no.nav.sikkerhetstjenesten.entraproxy.graph.Enhet.Enhetnummer
import no.nav.sikkerhetstjenesten.entraproxy.graph.Tema.Companion.TEMA_PREFIX
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
        with(graphClient.users("id", filter = "onPremisesSamAccountName eq '$navIdent'").oids) {
            log.info("Fant $size oids ($this) i Entra for $navIdent")
            when (size) {
                0 -> throw NotFoundRestException(cf.userURI(navIdent), msg = "Fant ingen oid for navident $navIdent, er den fremdeles gyldig?")
                1 -> singleOrNull()?.id
                else -> throw EntraOidException(navIdent, "Forventet nøyaktig én oid for navident $navIdent, fant $size (${joinToString(", ") { it.id.toString() }})")
            }
        }

    fun gruppeOid(gruppeNavn: String) =
        graphClient.groups("id,displayName", "displayName eq '$gruppeNavn'").value.firstOrNull()?.id

    fun tema(ansattOid: String): Set<Tema> =
        graphClient.memberOf(ansattOid, "id,displayName", "startswith(displayName,'$TEMA_PREFIX')")
            .value
            .mapTo(sortedSetOf()) { Tema(it.displayName) }

    fun enheter(ansattOid: String): Set<Enhetnummer> =
        graphClient.memberOf(ansattOid, "id,displayName", "startswith(displayName,'$ENHET_PREFIX')")
            .value
            .mapTo(sortedSetOf()) { Enhetnummer(it.displayName) }

    fun ansatteGrupper(ansattOid: String): Set<EntraGruppe> =
        graphClient.memberOf(ansattOid, "id,displayName")
            .value
            .mapTo(sortedSetOf()) { EntraGruppe(it.displayName) }

    fun gruppeMedlemmer(gruppeOid: String): Set<Ansatt> =
        graphClient.members(gruppeOid, "id, givenName, surname,displayName, onPremisesSamAccountName")
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
            "jobTitle,onPremisesSamAccountName,id,givenName,surname,displayName,mail,streetAddress",
            "onPremisesSamAccountName eq '$ansattId'"
        ).ansatte.firstOrNull()

    fun utvidetAnsattTident(ansattId: String) =
        graphClient.usersByFilter(
            "jobTitle,onPremisesSamAccountName,id,givenName,surname,displayName,mail,streetAddress",
            "jobTitle eq '$ansattId'"
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

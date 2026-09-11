package no.nav.sikkerhetstjenesten.entraproxy.graph

import io.opentelemetry.api.trace.Span
import no.nav.sikkerhetstjenesten.entraproxy.felles.rest.NotFoundRestException
import no.nav.sikkerhetstjenesten.entraproxy.felles.rest.Pingable
import no.nav.sikkerhetstjenesten.entraproxy.graph.Enhet.Companion.ENHET_PREFIX
import no.nav.sikkerhetstjenesten.entraproxy.graph.Enhet.Enhetnummer
import no.nav.sikkerhetstjenesten.entraproxy.graph.EntraConfig.Companion.GRAPH
import no.nav.sikkerhetstjenesten.entraproxy.graph.EntraConfig.Companion.NAVIDENT
import no.nav.sikkerhetstjenesten.entraproxy.graph.Tema.Companion.TEMA_PREFIX
import no.nav.sikkerhetstjenesten.entraproxy.norg.NorgProxyClient
import no.nav.sikkerhetstjenesten.entraproxy.norg.NorgProxyClient.Companion.NORG
import org.slf4j.LoggerFactory.getLogger
import org.springframework.http.HttpStatus.NOT_FOUND
import org.springframework.stereotype.Component
import org.springframework.web.ErrorResponseException
import org.springframework.web.service.registry.ImportHttpServices

@Component
@ImportHttpServices(types = [EntraGraphClient::class], group = GRAPH)
class EntraRestClientAdapter(private val client: EntraGraphClient, val cf: EntraConfig) : Pingable {

    val log = getLogger(javaClass)

    override fun ping() = client.ping()
    override val name = cf.name
    override val pingEndpoint = "${cf.pingEndpoint}"

    val baseURI = cf.baseUri

    fun oidForAnsatt(navIdent: String) =
        with(client.users("id", filter = "$NAVIDENT eq '$navIdent'").oids) {
            log.info("Fant $size oids ($this) i Entra for $navIdent")
            when (size) {
                0 -> throw NotFoundRestException(cf.userURI(navIdent), msg = "Fant ingen oid for navident $navIdent, er den fremdeles gyldig?")
                1 -> singleOrNull()?.id
                else -> throw EntraOidException(navIdent, "Forventet nøyaktig én oid for navident $navIdent, fant $size (${joinToString(", ") { it.id.toString() }})")
            }
        }

    fun oidForGruppenavn(gruppeNavn: String) =
        client.groups("id,displayName", "displayName eq '$gruppeNavn'").value.firstOrNull()?.id

    fun temaerForAnsatt(ansattOid: String) =
        client.memberOf(ansattOid, "id,displayName", "startswith(displayName,'$TEMA_PREFIX')").value
            .mapTo(sortedSetOf()) { Tema(it.displayName) }

    fun enheterForAnsatt(ansattOid: String) : Set<Enhetnummer> =
        client.memberOf(ansattOid, "id,displayName", "startswith(displayName,'$ENHET_PREFIX')").value
            .mapTo(sortedSetOf()) { Enhetnummer(it.displayName) }

    fun grupperForAnsatt(ansattOid: String)  =
        client.memberOf(ansattOid, "id,displayName").value
            .mapTo(sortedSetOf()) { EntraGruppe(it.displayName) }

    fun gruppeMedlemmer(gruppeOid: String) : Set<Ansatt> =
        client.members(gruppeOid, "id,givenName,surname,displayName, $NAVIDENT").value
            .mapTo(sortedSetOf()) {
                with(it) {
                    Ansatt(AnsattId(onPremisesSamAccountName), displayName, givenName, surname)
                }
            }

    fun utvidetAnsatt(ansattId: String) =
        client.bruker(
            "jobTitle,$NAVIDENT,id,givenName,surname,displayName,mail,streetAddress",
            "onPremisesSamAccountName eq '$ansattId'"
        ).ansatte.firstOrNull()

    fun utvidetAnsattTident(ansattId: String) =
        client.bruker(
            "jobTitle,$NAVIDENT,id,givenName,surname,displayName,mail,streetAddress",
            "jobTitle eq '$ansattId'"
        ).ansatte.firstOrNull()

    override fun toString() = "${javaClass.simpleName} [client=$client, config=$cf]"
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

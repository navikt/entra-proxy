package no.nav.sikkerhetstjenesten.entraproxy.graph

import no.nav.sikkerhetstjenesten.entraproxy.felles.OAuth2DownstreamURIContext.currentUri
import no.nav.sikkerhetstjenesten.entraproxy.felles.cache.CacheOperations
import no.nav.sikkerhetstjenesten.entraproxy.felles.rest.NotFoundRestException
import no.nav.sikkerhetstjenesten.entraproxy.graph.MedlemmerConfig.Companion.MEDLEMMER
import no.nav.sikkerhetstjenesten.entraproxy.felles.rest.RetryingWhenRecoverable
import no.nav.sikkerhetstjenesten.entraproxy.graph.Enhet.Companion.ENHET_PREFIX
import no.nav.sikkerhetstjenesten.entraproxy.graph.Enhet.Enhetnummer
import no.nav.sikkerhetstjenesten.entraproxy.graph.EntraGraphClient.Companion.GRAPH
import no.nav.sikkerhetstjenesten.entraproxy.graph.EntraOidConfig.Companion.OID_CACHE
import no.nav.sikkerhetstjenesten.entraproxy.graph.EntraSaksbehandlerRespons.AnsattRespons
import no.nav.sikkerhetstjenesten.entraproxy.graph.Tema.Companion.TEMA_PREFIX
import no.nav.sikkerhetstjenesten.entraproxy.norg.NorgTjeneste
import org.slf4j.LoggerFactory.getLogger
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import java.util.*

private const val BRUKER = "onPremisesSamAccountName"
private const val MINIMUM_FELTER = "id,displayName"
private const val ANSATTE_FELTER = "$MINIMUM_FELTER,jobTitle,$BRUKER,givenName,surname,mail,streetAddress"

@RetryingWhenRecoverable
@Service
class EntraTjeneste(private val client: EntraGraphClient, private val norg: NorgTjeneste, private val oid: EntraOidTjeneste, private val cache: CacheOperations)  {

    private val log = getLogger(javaClass)

    @Cacheable(cacheNames = [GRAPH],  key = "#root.methodName + ':' + #ansattId.verdi")
    fun tema(ansattId: AnsattId, oid: UUID) =
        runCatching {
            temaerForAnsatt("$oid")
        }.getOrElse {
            if (it is NotFoundRestException)  {
                temaerForAnsatt("${refreshOid(ansattId)}")
            }
            else throw it
        }


    @Cacheable(cacheNames = [GRAPH],  key = "#root.methodName + ':' + #ansattId.verdi")
    fun enheter(ansattId: AnsattId, oid: UUID) =
        runCatching {
            enheter(oid)
        }.getOrElse {
            if (it is NotFoundRestException)  {
                enheter(refreshOid(ansattId))
            }
            else throw it
        }


    @Cacheable(MEDLEMMER, key = "#gruppeId.toString()")
    fun medlemmerIGruppe(gruppeId: UUID) =
            gruppeMedlemmer("$gruppeId")


    @Cacheable(GRAPH,key = "#root.methodName + ':' + #ansattId.verdi")
    fun utvidetAnsatt(ansattId: AnsattId) =
        ansatt  {
            client.bruker(ANSATTE_FELTER, "$BRUKER eq '${ansattId.verdi}'").ansatte.firstOrNull()
        }

    @Cacheable(GRAPH,key = "#root.methodName + ':' + #ansattId.verdi")
    fun utvidetAnsatt(ansattId: TIdent) =
        ansatt  {
            client.bruker(ANSATTE_FELTER, "jobTitle eq '${ansattId.verdi}'").ansatte.firstOrNull()
        }


    @Cacheable(GRAPH,key = "#root.methodName + ':' + #navIdent")
    fun grupperForAnsatt(navIdent: AnsattId, oid: UUID) =
        runCatching {
            grupperForAnsatt(oid.toString())
        }.getOrElse {
            if (it is NotFoundRestException)  {
                grupperForAnsatt(refreshOid(navIdent).toString())
            }
            else throw it
        }

    private fun temaerForAnsatt(ansattOid: String) =
        client.memberOf(ansattOid, MINIMUM_FELTER, "startswith(displayName,'$TEMA_PREFIX')").value
            .mapTo(sortedSetOf()) { Tema(it.displayName) }

    private fun grupperForAnsatt(ansattOid: String) =
        client.memberOf(ansattOid, MINIMUM_FELTER).value
            .mapTo(sortedSetOf()) { EntraGruppe(it.displayName) }

    private fun gruppeMedlemmer(gruppeOid: String): Set<Ansatt> =
        client.members(gruppeOid, ANSATTE_FELTER).value
            .mapTo(sortedSetOf()) {
                with(it) {
                    Ansatt(AnsattId(onPremisesSamAccountName), displayName, givenName, surname)
                }
            }

    private fun enheter(ansattOid: UUID) =
        buildSet {
            client.memberOf("$ansattOid", MINIMUM_FELTER, "startswith(displayName,'$ENHET_PREFIX')").value
                .map { Enhetnummer(it.displayName) }
                .forEach {
                    add(Enhet(it, norg.navnFor(it)))
                }
        }


    private fun ansatt(block: () -> AnsattRespons?) =
        block()?.let {
            with(it) {
                val enhetsNummer = Enhetnummer(streetAddress?: UKJENT_ENHET)
                UtvidetAnsatt(
                    AnsattId(onPremisesSamAccountName), displayName, givenName, surname,
                    TIdent(jobTitle?: TIDENT_DEFAULT),
                    mail,
                    Enhet(enhetsNummer, norg.navnFor(enhetsNummer)))
            }
        }

    private fun refreshOid(navIdent: AnsattId): UUID {
        cache.delete(OID_CACHE,navIdent.verdi).also {
            log.info("Slettet cache innslag før henting av ny oid $navIdent")
        }
        return oid.ansattOid(navIdent).also {
            log.info("Hentet  ny oid $it for $navIdent")
        }
            ?: throw NotFoundRestException(currentUri, "Fant ikke oid for ${navIdent.verdi} i Entra, selv etter cache-opprydding")
    }

    override fun toString() =
        "${javaClass.simpleName} [client=$client, norg=$norg]"

}


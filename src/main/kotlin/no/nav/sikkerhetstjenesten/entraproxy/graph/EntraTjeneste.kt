package no.nav.sikkerhetstjenesten.entraproxy.graph

import no.nav.sikkerhetstjenesten.entraproxy.felles.OAuth2DownstreamURIContext.currentUri
import no.nav.sikkerhetstjenesten.entraproxy.felles.cache.CacheInaktiveNavIdenter.Companion.INAKTIVE
import no.nav.sikkerhetstjenesten.entraproxy.felles.cache.CacheOperations
import no.nav.sikkerhetstjenesten.entraproxy.felles.rest.NotFoundRestException
import no.nav.sikkerhetstjenesten.entraproxy.felles.rest.RestRetryingWhenRecoverableService
import no.nav.sikkerhetstjenesten.entraproxy.graph.AnsattId.Companion.ANSATTID_LENGTH
import no.nav.sikkerhetstjenesten.entraproxy.graph.Enhet.Companion.ENHET_PREFIX
import no.nav.sikkerhetstjenesten.entraproxy.graph.Enhet.Enhetnummer
import no.nav.sikkerhetstjenesten.entraproxy.graph.EntraGraphClient.Companion.GRAPH
import no.nav.sikkerhetstjenesten.entraproxy.graph.EntraOidConfig.Companion.OID_CACHE
import no.nav.sikkerhetstjenesten.entraproxy.graph.EntraSaksbehandlerRespons.AnsattRespons
import no.nav.sikkerhetstjenesten.entraproxy.graph.MedlemmerConfig.Companion.MEDLEMMER
import no.nav.sikkerhetstjenesten.entraproxy.graph.Tema.Companion.TEMA_PREFIX
import no.nav.sikkerhetstjenesten.entraproxy.norg.NorgTjeneste
import org.slf4j.LoggerFactory.getLogger
import org.springframework.cache.annotation.Cacheable
import java.net.URI
import java.util.UUID

const val BRUKER = "onPremisesSamAccountName"
private const val MINIMUM_FELTER = "id,displayName"
private const val ANSATTE_FELTER = "$MINIMUM_FELTER,jobTitle,$BRUKER,givenName,surname,mail,streetAddress"

@RestRetryingWhenRecoverableService
class EntraTjeneste(private val client: EntraGraphClient, private val norg: NorgTjeneste, private val oid: EntraOidTjeneste, private val cache: CacheOperations)  {

    private val log = getLogger(javaClass)

    @Cacheable(cacheNames = [GRAPH],  key = "#root.methodName + ':' + #ansattId.verdi")
    fun tema(ansattId: AnsattId, oid: UUID) =
        runCatching {
            if (cache.inneholder(ansattId)) {
                emptySet()
            }
            else  {
                temaerForAnsatt(ansattId,"$oid").also {
                    log.info("Hentet ${it.size} tema for ansatt $ansattId ($oid)")
                }
            }
        }.getOrElse {
            if (it is NotFoundRestException)  {
                val nyOid = refreshOid(ansattId)
                temaerForAnsatt(ansattId,"$nyOid").also {
                    log.info("Hentet ${it.size} tema for ansatt $ansattId etter refresh oid til $nyOid")
                }
            }
            else throw it
        }



    @Cacheable(cacheNames = [GRAPH],  key = "#root.methodName + ':' + #ansattId.verdi")
    fun enheter(ansattId: AnsattId, oid: UUID) =
        runCatching {
            if (cache.inneholder(ansattId)) {
                emptySet()
            }
            else  {
                enheter(oid).also {
                    log.info("Hentet ${it.size} enhet(er) for ansatt $ansattId ($oid)")
                }
            }
        }.getOrElse {
            if (it is NotFoundRestException)  {
                val nyOid = refreshOid(ansattId)
                enheter(nyOid).also {
                    log.info("Hentet ${it.size} enhet(er) for ansatt $ansattId etter refresh oid til $nyOid")
                }
            }
            else throw it
        }


    @Cacheable(MEDLEMMER, key = "#gruppeId.toString()")
    fun medlemmerIGruppe(gruppeNavn: String, gruppeId: UUID) =
            gruppeMedlemmer("$gruppeId").also {
                log.info("Hentet ${it.size} medlem(mer) for gruppe $gruppeNavn ($gruppeId)")
            }


    @Cacheable(GRAPH,key = "#root.methodName + ':' + #ansattId.verdi")
    fun utvidetAnsatt(ansattId: AnsattId) =
        ansatt  {
            client.bruker(ANSATTE_FELTER, "$BRUKER eq '${ansattId.verdi}'").ansatte.firstOrNull()
        }?.also {
            log.info("Hentet ansatt $it for ident ${ansattId.verdi}: $it")
        }

    @Cacheable(GRAPH,key = "#root.methodName + ':' + #ansattId.verdi")
    fun utvidetAnsatt(ansattId: TIdent) =
        ansatt  {
            client.bruker(ANSATTE_FELTER, "jobTitle eq '${ansattId.verdi}'").ansatte.firstOrNull()
        }?.also {
            log.info("Hentet utvidet ansatt $it for tIdent ${ansattId.verdi}: $it")
        }


    @Cacheable(GRAPH,key = "#root.methodName + ':' + #navIdent")
    fun grupperForAnsatt(navIdent: AnsattId, oid: UUID) =
        runCatching {
            grupperForAnsatt(navIdent,"$oid").also {
                log.info("Hentet ${it.size} gruppe(r) for ansatt ${navIdent.verdi} ($oid)")
            }
        }.getOrElse {
            if (it is NotFoundRestException)  {
                val nyOid = refreshOid(navIdent)
                grupperForAnsatt(navIdent,"$nyOid").also {
                    log.info("Hentet ${it.size} gruppe(r) for ansatt ${navIdent.verdi} etter refresh oid til $nyOid")
                }
            }
            else throw it
        }

    private fun temaerForAnsatt(ansattId: AnsattId, ansattOid: String) =
        allSider("temaer for ${ansattId.verdi} ($ansattOid)", client.memberOf(ansattOid, MINIMUM_FELTER, "startswith(displayName,'$TEMA_PREFIX')"), Tilganger::next, client::tilgangerSide)
            .flatMap { it.value }
            .mapTo(sortedSetOf()) {
                Tema(it.displayName)
            }

    private fun grupperForAnsatt(ansattId: AnsattId,ansattOid: String) =
        allSider("grupper for ${ansattId.verdi} ($ansattOid)", client.memberOf(ansattOid, MINIMUM_FELTER), Tilganger::next, client::tilgangerSide)
            .flatMap { it.value }
            .mapTo(sortedSetOf()) {
                EntraGruppe(it.displayName)
            }

     fun gruppeMedlemmer(oid: String): Set<Ansatt> {
        val alleMedlemmer = allSider("medlemmer av gruppe $oid", client.members(oid, ANSATTE_FELTER), GruppeMedlemmer::next, client::gruppeMedlemmerSide)
            .flatMap { it.value }
        val (gyldigeMedlemmer, ugyldigeMedlemmer) = alleMedlemmer.partition {
            it.onPremisesSamAccountName?.length == ANSATTID_LENGTH
        }
        if (ugyldigeMedlemmer.isNotEmpty()) {
            log.info("Ignorerte {} medlem(mer) fra gruppe {} uten gyldig onPremisesSamAccountName (f.eks. nøstede grupper eller tjenestekontoer)",
                ugyldigeMedlemmer, oid)
        }
        return gyldigeMedlemmer.mapTo(sortedSetOf()) {
            with(it) {
                Ansatt(AnsattId(onPremisesSamAccountName!!), displayName, givenName, surname)
            }
        }
    }

    private fun enheter(ansattOid: UUID) =
        buildSet {
            allSider("enheter for $ansattOid", client.memberOf("$ansattOid", MINIMUM_FELTER, "startswith(displayName,'$ENHET_PREFIX')"), Tilganger::next, client::tilgangerSide)
                .flatMap { it.value }
                .map {
                    Enhetnummer(it.displayName)
                }
                .forEach {
                    add(Enhet(it, norg.navnFor(it)))
                }
        }

    private fun <T> allSider(beskrivelse: String, førsteSide: T, next: (T) -> URI?, hentSide: (URI) -> T): List<T> {
        log.info("Henter {}", beskrivelse)
        var sideNummer = 1
        val sider = generateSequence(førsteSide) { side ->
            next(side)?.let { nesteSideUri ->
                sideNummer++
                log.trace("Følger @odata.nextLink for {}, side {}", beskrivelse, sideNummer)
                hentSide(nesteSideUri)
            }
        }.toList()
        return sider
    }



    private fun ansatt(block: () -> AnsattRespons?) =
        block()?.let { respons ->
            respons.onPremisesSamAccountName?.let { navIdent ->
                with(respons) {
                    val enhetsNummer = Enhetnummer(streetAddress?: UKJENT_ENHET)
                    UtvidetAnsatt(
                        AnsattId(navIdent), displayName, givenName, surname,
                        TIdent(jobTitle?: TIDENT_DEFAULT),
                        mail,
                        Enhet(enhetsNummer, norg.navnFor(enhetsNummer)))
                }
            }
        }

    private fun CacheOperations.inneholder(ansattId: AnsattId) =
        inneholder(INAKTIVE, ansattId.verdi)

    private fun refreshOid(navIdent: AnsattId): UUID {
        cache.delete(OID_CACHE,navIdent.verdi).also {
            log.info("Slettet cache innslag før henting av ny oid $navIdent")
        }
        return oid.ansattOid(navIdent).also {
            log.info("Hentet ny oid $it for $navIdent")
        } ?: throw NotFoundRestException(currentUri, "Fant ikke ny oid for $navIdent i Entra, selv etter cache-opprydding")
    }

    override fun toString() =
        "${javaClass.simpleName} [client=$client, norg=$norg]"

}

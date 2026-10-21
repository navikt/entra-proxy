package no.nav.sikkerhetstjenesten.entraproxy.graph

import no.nav.sikkerhetstjenesten.entraproxy.cache.CacheInaktiveNavIdenter.Companion.INAKTIVE
import no.nav.sikkerhetstjenesten.entraproxy.graph.Enhet.Companion.ENHET_PREFIX
import no.nav.sikkerhetstjenesten.entraproxy.graph.Enhet.Enhetnummer
import no.nav.sikkerhetstjenesten.entraproxy.graph.EntraOidConfig.Companion.OID_CACHE
import no.nav.sikkerhetstjenesten.entraproxy.graph.Tema.Companion.TEMA_PREFIX
import no.nav.sikkerhetstjenesten.entraproxy.norg.NorgTjeneste
import no.nav.sikkerhetstjenesten.felles.cache.CacheOperations
import no.nav.sikkerhetstjenesten.felles.rest.DownstreamURIContext.currentUri
import no.nav.sikkerhetstjenesten.felles.rest.NotFoundRestException
import org.slf4j.LoggerFactory.getLogger
import java.net.URI
import java.util.UUID

class EntraGraphAdapter(private val client: EntraGraphClient, private val norg: NorgTjeneste, private val cache: CacheOperations, private val oid: EntraOidTjeneste) : EntraGraphOperations {

    private val log = getLogger(javaClass)

    override fun utvidetAnsatt(ansattId: AnsattId): UtvidetAnsatt? =
        utvidetAnsatt(BRUKER_FILTER.format(ansattId.verdi), ansattId.verdi)

    override fun utvidetAnsatt(ident: TIdent): UtvidetAnsatt? =
        utvidetAnsatt(TIDENT_FILTER.format(ident.verdi), ident.verdi)

    override fun temaer(ansattId: AnsattId, ansattOid: UUID): Set<Tema> {
        if (erInaktiv(ansattId)) return emptySet()
        return medOidOppfrisking(ansattId, ansattOid) { oid ->
            allSider("temaer for $ansattId ($oid)", client.memberOf("$oid", MINIMUM_FELTER, "startswith(displayName,'$TEMA_PREFIX')"), Tilganger::next, client::tilgangerSide)
                .flatMapTo(mutableSetOf()) { it.value }
                .mapTo(sortedSetOf()) { Tema(it.displayName) }
                .also { log.info("Hentet ${it.size} tema for $ansattId ($oid)") }
        }
    }

    override fun grupper(ansattId: AnsattId, ansattOid: UUID): Set<EntraGruppe> =
        medOidOppfrisking(ansattId, ansattOid) { oid ->
            allSider("grupper for ${ansattId.verdi} ($oid)", client.memberOfGrupper("$oid", MINIMUM_FELTER_FILTRERT, FILTER_STRING), Tilganger::next, client::tilgangerSide)
                .flatMapTo(mutableSetOf()) { it.value }
                .mapTo(sortedSetOf()) { EntraGruppe(it.displayName, it.id, it.description) }
                .also { log.info("Hentet ${it.size} gruppe(r) for $ansattId ($oid)") }
        }

    override fun enheter(ansattId: AnsattId, ansattOid: UUID): Set<Enhet> {
        if (erInaktiv(ansattId)) return emptySet()
        return medOidOppfrisking(ansattId, ansattOid) { oid ->
            allSider("enheter for $oid", client.memberOf("$oid", MINIMUM_FELTER, "startswith(displayName,'$ENHET_PREFIX')"), Tilganger::next, client::tilgangerSide)
                .flatMapTo(mutableSetOf()) { it.value }
                .mapTo(mutableSetOf()) {
                    val enhetsnummer = Enhetnummer(it.displayName)
                    Enhet(enhetsnummer, norg.navn(enhetsnummer))
                }
                .also { log.info("Hentet ${it.size} enhet(er) for $ansattId ($oid)") }
        }
    }

    override fun gruppeMedlemmer(oid: String, gruppeNavn: String): Set<Ansatt> {
        val alleMedlemmer = allSider("medlemmer av gruppe $oid ($gruppeNavn)", client.members(oid, ANSATTE_FELTER), GruppeMedlemmer::next, client::gruppeMedlemmerSide)
            .flatMapTo(mutableSetOf()) { it.value }
        val (gyldigeMedlemmer, ugyldigeMedlemmer) = alleMedlemmer.partition {
            it.onPremisesSamAccountName?.erAnsattId() == true
        }
        if (ugyldigeMedlemmer.isNotEmpty()) {
            log.info("Ignorerte {} ugyldige medlem(mer) fra gruppe {} uten gyldig onPremisesSamAccountName (\n${ugyldigeMedlemmer.joinToString("\n")})",
                ugyldigeMedlemmer.size, oid)
        }
        return gyldigeMedlemmer.mapTo(sortedSetOf()) {
            with(it) {
                Ansatt(AnsattId(onPremisesSamAccountName!!), displayName, givenName, surname)
            }
        }.also { log.info("Hentet ${it.size} medlem(mer) for gruppe $gruppeNavn ($oid)") }
    }

    override fun antallMedlemmer(vararg gruppeIder: UUID): Map<UUID, Long> =
        gruppeIder.asList()
            .chunked(BATCH_STØRRELSE)
            .flatMap { antallForBatch(it).toList() }
            .toMap()

    override fun toString() =
        "${javaClass.simpleName} [client=$client]"

    private fun utvidetAnsatt(filter: String, verdi: String): UtvidetAnsatt? =
        client.bruker(ANSATTE_FELTER, filter).ansatte.firstOrNull()?.let { respons ->
            with(respons) {
                onPremisesSamAccountName?.let { navIdent ->
                    val enhetsNummer = Enhetnummer(streetAddress ?: UKJENT_ENHET)
                    UtvidetAnsatt(
                        AnsattId(navIdent),
                        displayName,
                        givenName,
                        surname,
                        TIdent(jobTitle ?: TIDENT_DEFAULT),
                        mail,
                        Enhet(enhetsNummer, norg.navn(enhetsNummer))
                    ).also {
                        log.trace("Hentet utvidet ansatt {} for ident {}", it, verdi)
                    }
                }
            }
        }

    private fun <T> medOidOppfrisking(ansattId: AnsattId, ansattOid: UUID, oppslag: (UUID) -> T): T =
        runCatching {
            oppslag(ansattOid)
        }.getOrElse {
            if (it is NotFoundRestException) {
                oppslag(nyOid(ansattId))
            }
            else throw it
        }

    private fun nyOid(navIdent: AnsattId): UUID {
        cache.delete(OID_CACHE, navIdent.verdi).also {
            log.info("Slettet cache innslag før henting av ny oid $navIdent")
        }
        return oid.ansattOid(navIdent) ?: throw NotFoundRestException(currentUri, "Fant ikke ny oid for $navIdent i Entra, selv etter cache-opprydding og nytt oppslag")
    }

    private fun erInaktiv(ansattId: AnsattId) =
        cache.inneholder(INAKTIVE, ansattId.verdi).also {
            if (it) {
                log.info("$ansattId er inaktiv")
            }
        }

    private fun antallForBatch(gruppeIder: List<UUID>): Map<UUID, Long> {
        val idTilGruppe = gruppeIder.mapIndexed { index, gruppeId -> "$index" to gruppeId }.toMap()
        val forespørsler = idTilGruppe.map { (id, gruppeId) ->
            BatchSubRequest(id, $$"/groups/$$gruppeId/members/$count")
        }
        return client.batch(BatchRequest(forespørsler)).responses
            .mapNotNull { respons ->
                val gruppeId = idTilGruppe[respons.id] ?: return@mapNotNull null
                val antall = (respons.body as? Number)?.toLong()
                if (respons.status == 200 && antall != null) {
                    gruppeId to antall
                } else {
                    log.warn("Kunne ikke hente antall medlemmer for gruppe $gruppeId (status ${respons.status})")
                    null
                }
            }.toMap()
    }

    private fun <T> allSider(beskrivelse: String, førsteSide: T, next: (T) -> URI?, hentSide: (URI) -> T): Set<T> {
        log.info("Henter {}", beskrivelse)
        var sideNummer = 1
        return generateSequence(førsteSide) { side ->
            next(side)?.let { nesteSideUri ->
                sideNummer++
                log.trace("Følger $NEXT_LINK for {}, side {}", beskrivelse, sideNummer)
                hentSide(nesteSideUri)
            }
        }.toSet().also {
            log.info("Hentet {} side(r) for {}", it.size, beskrivelse)
        }
    }

    companion object {
        const val BRUKER = "onPremisesSamAccountName"
        private const val MINIMUM_FELTER = "id,displayName"
        private const val MINIMUM_FELTER_FILTRERT = "$MINIMUM_FELTER,securityEnabled,mailEnabled,description"
        private const val ANSATTE_FELTER = "$MINIMUM_FELTER,jobTitle,${BRUKER},givenName,surname,mail,streetAddress"
        private const val BRUKER_FILTER = "$BRUKER eq '%s'"
        private const val TIDENT_FILTER = "jobTitle eq '%s'"
        private const val FILTER_STRING = "securityEnabled eq true and mailEnabled eq false"
        private const val BATCH_STØRRELSE = 20
    }
}

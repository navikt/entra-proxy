package no.nav.sikkerhetstjenesten.entraproxy.graph

import no.nav.sikkerhetstjenesten.entraproxy.graph.Enhet.Companion.ENHET_PREFIX
import no.nav.sikkerhetstjenesten.entraproxy.graph.Enhet.Enhetnummer
import no.nav.sikkerhetstjenesten.entraproxy.graph.Tema.Companion.TEMA_PREFIX
import no.nav.sikkerhetstjenesten.entraproxy.norg.NorgTjeneste
import org.slf4j.LoggerFactory.getLogger
import java.net.URI
import java.util.UUID

class EntraGraphAdapter(private val client: EntraGraphClient, private val norg: NorgTjeneste) : EntraGraphOperations {

    private val log = getLogger(javaClass)

    override fun utvidetAnsatt(ansattId: AnsattId): UtvidetAnsatt? =
        utvidetAnsatt(BRUKER_FILTER.format(ansattId.verdi), ansattId.verdi)

    override fun utvidetAnsatt(ident: TIdent): UtvidetAnsatt? =
        utvidetAnsatt(TIDENT_FILTER.format(ident.verdi), ident.verdi)

    override fun temaer(ansattOid: UUID): Set<Tema> =
        allSider("temaer for $ansattOid", client.memberOf("$ansattOid", MINIMUM_FELTER, "startswith(displayName,'$TEMA_PREFIX')"), Tilganger::next, client::tilgangerSide)
            .flatMapTo(mutableSetOf()) { it.value }
            .mapTo(sortedSetOf()) { Tema(it.displayName) }
            .also { log.info("Hentet ${it.size} tema for $ansattOid") }

    override fun grupper(ansattOid: UUID): Set<EntraGruppe> =
        allSider("grupper for $ansattOid", client.memberOfGrupper("$ansattOid", MINIMUM_FELTER_FILTRERT, FILTER_STRING), Tilganger::next, client::tilgangerSide)
            .flatMapTo(mutableSetOf()) { it.value }
            .mapTo(sortedSetOf()) { EntraGruppe(it.displayName, it.id, it.description) }
            .also { log.info("Hentet ${it.size} gruppe(r) for $ansattOid") }

    override fun enheter(ansattOid: UUID): Set<Enhet> =
        allSider("enheter for $ansattOid", client.memberOf("$ansattOid", MINIMUM_FELTER, "startswith(displayName,'$ENHET_PREFIX')"), Tilganger::next, client::tilgangerSide)
            .flatMapTo(mutableSetOf()) { it.value }
            .mapTo(mutableSetOf()) {
                val enhetsnummer = Enhetnummer(it.displayName)
                Enhet(enhetsnummer, norg.navn(enhetsnummer))
            }
            .also { log.info("Hentet ${it.size} enhet(er) for $ansattOid") }

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

package no.nav.sikkerhetstjenesten.entraproxy.graph.client

import no.nav.sikkerhetstjenesten.entraproxy.graph.Ansatt
import no.nav.sikkerhetstjenesten.entraproxy.graph.AnsattId
import no.nav.sikkerhetstjenesten.entraproxy.graph.Enhet.Enhetnummer
import no.nav.sikkerhetstjenesten.entraproxy.graph.EntraGruppe
import no.nav.sikkerhetstjenesten.entraproxy.graph.TIdent
import no.nav.sikkerhetstjenesten.entraproxy.graph.Tema
import no.nav.sikkerhetstjenesten.entraproxy.graph.UtvidetAnsatt
import java.util.UUID

interface EntraGraphOperations {

    fun utvidetAnsatt(ansattId: AnsattId): UtvidetAnsatt?

    fun utvidetAnsatt(ident: TIdent): UtvidetAnsatt?

    fun temaer(ansattOid: UUID): Set<Tema>

    fun grupper(ansattOid: UUID): Set<EntraGruppe>

    fun grupperSomMatcher(subString: String): Set<EntraGruppe>

    fun enheter(ansattOid: UUID): Set<Enhetnummer>

    fun gruppeMedlemmer(oid: String): Set<Ansatt>

    fun antallMedlemmer(vararg gruppeIder: UUID): Map<UUID, Long>

    fun validerGrupper(vararg gruppeIder: UUID)
}

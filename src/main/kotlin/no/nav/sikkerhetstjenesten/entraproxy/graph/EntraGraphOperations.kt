package no.nav.sikkerhetstjenesten.entraproxy.graph

import no.nav.sikkerhetstjenesten.entraproxy.graph.Enhet.Enhetnummer
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
}

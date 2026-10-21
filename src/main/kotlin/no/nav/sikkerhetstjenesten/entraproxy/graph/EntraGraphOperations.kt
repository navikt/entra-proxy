package no.nav.sikkerhetstjenesten.entraproxy.graph

import java.util.UUID

interface EntraGraphOperations {

    fun utvidetAnsatt(ansattId: AnsattId): UtvidetAnsatt?

    fun utvidetAnsatt(ident: TIdent): UtvidetAnsatt?

    fun temaer(ansattOid: UUID): Set<Tema>

    fun grupper(ansattOid: UUID): Set<EntraGruppe>

    fun enheter(ansattOid: UUID): Set<Enhet.Enhetnummer>

    fun gruppeMedlemmer(oid: String, gruppeNavn: String): Set<Ansatt>

    fun antallMedlemmer(vararg gruppeIder: UUID): Map<UUID, Long>
}

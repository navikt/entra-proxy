package no.nav.sikkerhetstjenesten.entraproxy.graph

import java.util.UUID

interface EntraGraphOperations {

    fun utvidetAnsatt(ansattId: AnsattId): UtvidetAnsatt?

    fun utvidetAnsatt(ident: TIdent): UtvidetAnsatt?

    fun temaer(ansattId: AnsattId, ansattOid: UUID): Set<Tema>

    fun grupper(ansattId: AnsattId, ansattOid: UUID): Set<EntraGruppe>

    fun enheter(ansattId: AnsattId, ansattOid: UUID): Set<Enhet>

    fun gruppeMedlemmer(oid: String, gruppeNavn: String): Set<Ansatt>

    fun antallMedlemmer(vararg gruppeIder: UUID): Map<UUID, Long>
}

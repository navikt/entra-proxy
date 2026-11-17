package no.nav.sikkerhetstjenesten.entraproxy.graph.tjeneste

import no.nav.sikkerhetstjenesten.entraproxy.graph.AnsattId
import java.util.UUID

interface EntraOidOperations {

    fun ansattOid(ansattId: AnsattId): UUID?

    fun oppfriskOid(ansattId: AnsattId): UUID
}

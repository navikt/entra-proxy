package no.nav.sikkerhetstjenesten.entraproxy.graph

import java.util.UUID

interface EntraOidOperations {

    fun ansattOid(ansattId: AnsattId): UUID?

    fun nyOid(ansattId: AnsattId): UUID
}

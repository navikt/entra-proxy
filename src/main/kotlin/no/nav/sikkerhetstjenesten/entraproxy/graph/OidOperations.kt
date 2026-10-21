package no.nav.sikkerhetstjenesten.entraproxy.graph

import java.util.UUID

interface OidOperations {

    fun ansattOid(ansattId: AnsattId): UUID?

    fun nyOid(ansattId: AnsattId): UUID
}

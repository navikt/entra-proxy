package no.nav.sikkerhetstjenesten.entraproxy.graph.tjeneste

import no.nav.sikkerhetstjenesten.entraproxy.graph.AnsattId

interface InaktivOperations {

    fun erInaktiv(ansattId: AnsattId): Boolean
}

package no.nav.sikkerhetstjenesten.entraproxy.graph

interface InaktivOperations {

    fun erInaktiv(ansattId: AnsattId): Boolean
}

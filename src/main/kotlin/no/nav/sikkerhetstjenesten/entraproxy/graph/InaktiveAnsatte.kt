package no.nav.sikkerhetstjenesten.entraproxy.graph

interface InaktiveAnsatte {

    fun erInaktiv(ansattId: AnsattId): Boolean
}

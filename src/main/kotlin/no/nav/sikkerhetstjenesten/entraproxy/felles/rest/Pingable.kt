package no.nav.sikkerhetstjenesten.entraproxy.felles.rest

interface Pingable {

    fun ping(): Any?
    val pingEndpoint: String
    val name: String
    val isEnabled get(): Boolean = true
}
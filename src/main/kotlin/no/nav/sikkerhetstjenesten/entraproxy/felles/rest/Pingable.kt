package no.nav.sikkerhetstjenesten.entraproxy.felles.rest

import java.net.URI

interface Pingable {

    fun ping(): Any?
    val pingEndpoint: URI
    val name: String
}
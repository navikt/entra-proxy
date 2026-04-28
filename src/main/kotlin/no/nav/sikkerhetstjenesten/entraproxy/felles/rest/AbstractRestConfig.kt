package no.nav.sikkerhetstjenesten.entraproxy.felles.rest


import org.springframework.web.util.DefaultUriBuilderFactory
import java.net.URI

abstract class AbstractRestConfig(val baseUri: URI, private val pingPath: String, val name: String) {

    protected fun builder() =
        DefaultUriBuilderFactory("$baseUri").builder()

    val pingEndpoint = builder().path(pingPath).build()

    override fun toString() =
        "name=$name, pingPath=$pingPath,baseUri=$baseUri"
}
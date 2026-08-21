package no.nav.sikkerhetstjenesten.entraproxy.felles.rest

import no.nav.sikkerhetstjenesten.entraproxy.felles.NoCoverageAnalysis
import org.springframework.web.util.DefaultUriBuilderFactory
import java.net.URI

abstract class RestConfig(val baseUri: URI, pingPath: String, val name: String) {

    protected fun builder() = DefaultUriBuilderFactory("$baseUri").builder()

    protected fun uri(path: String, vararg args: String) = builder().path(path).build(*args)

    val pingEndpoint = builder().path(pingPath).build()

    @NoCoverageAnalysis
    override fun toString() = "${javaClass.simpleName} [name=$name, pingEndpoint=$pingEndpoint,baseUri=$baseUri]"
}
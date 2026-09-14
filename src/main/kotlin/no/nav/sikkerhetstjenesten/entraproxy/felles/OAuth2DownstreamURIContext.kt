package no.nav.sikkerhetstjenesten.entraproxy.felles

import java.net.URI

object OAuth2DownstreamURIContext {
    private val downstreamUri = ThreadLocal<String>()

    val currentUri get() = URI.create(downstreamUri.get())

    fun set(uri: String) {
        downstreamUri.set(uri)
    }

    fun clear() {
        downstreamUri.remove()
    }
}
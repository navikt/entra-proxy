package no.nav.sikkerhetstjenesten.entraproxy.felles

import no.nav.sikkerhetstjenesten.felles.rest.DownstreamUriCapturingInterceptor
import org.springframework.beans.factory.ObjectProvider
import org.springframework.http.client.ClientHttpRequestInterceptor
import org.springframework.stereotype.Component
import org.zalando.logbook.spring.LogbookClientHttpRequestInterceptor

@Component
class FellesHttpClientInterceptors(
    private val logbook: ObjectProvider<LogbookClientHttpRequestInterceptor>,
) {
    fun configure(interceptors: MutableList<ClientHttpRequestInterceptor>) {
        interceptors.addFirst(DownstreamUriCapturingInterceptor())
        logbook.ifAvailable { interceptors.add(it) }
    }
}

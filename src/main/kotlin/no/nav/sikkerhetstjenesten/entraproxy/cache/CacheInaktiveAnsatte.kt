package no.nav.sikkerhetstjenesten.entraproxy.cache

import no.nav.sikkerhetstjenesten.entraproxy.cache.CacheInaktiveNavIdenter.Companion.INAKTIVE
import no.nav.sikkerhetstjenesten.entraproxy.graph.AnsattId
import no.nav.sikkerhetstjenesten.entraproxy.graph.InaktivOperations
import no.nav.sikkerhetstjenesten.felles.cache.CacheOperations
import org.springframework.stereotype.Component

@Component
class CacheInaktiveAnsatte(private val cache: CacheOperations) : InaktivOperations {

    override fun erInaktiv(ansattId: AnsattId) =
        cache.inneholder(INAKTIVE, ansattId.verdi)

    override fun toString() =
        "${javaClass.simpleName} [cache=$cache]"
}

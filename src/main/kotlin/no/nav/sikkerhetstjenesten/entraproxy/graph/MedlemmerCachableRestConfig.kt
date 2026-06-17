package no.nav.sikkerhetstjenesten.entraproxy.graph

import no.nav.sikkerhetstjenesten.entraproxy.felles.cache.CacheNøkkelConfig
import no.nav.sikkerhetstjenesten.entraproxy.felles.rest.CachableRestConfig
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.time.Duration

@Component
class MedlemmerCachableRestConfig(@param:Value("\${medlemmer.varighet:3h}") override val varighet: Duration) :
    CachableRestConfig {
    override val navn = MEDLEMMER
    override val caches = setOf(MEDLEMMER_CACHE)

    companion object {
        const val MEDLEMMER = "medlemmer"
        val MEDLEMMER_CACHE = CacheNøkkelConfig(MEDLEMMER)
    }
}
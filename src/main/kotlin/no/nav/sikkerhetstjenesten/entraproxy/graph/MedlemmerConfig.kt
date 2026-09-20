package no.nav.sikkerhetstjenesten.entraproxy.graph

import no.nav.sikkerhetstjenesten.entraproxy.felles.cache.CachableRestConfig
import no.nav.sikkerhetstjenesten.entraproxy.felles.cache.CacheNøkkelConfig
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.time.Duration

@Component
class MedlemmerConfig(@param:Value("\${medlemmer.varighet:3h}") override val varighet: Duration) :
    CachableRestConfig {
    override val navn = MEDLEMMER
    override val caches: Set<CacheNøkkelConfig>
        get() = setOf(MEDLEMMER_CACHE)

    companion object {
        val MEDLEMMER_CACHE = CacheNøkkelConfig(MEDLEMMER)
        const val MEDLEMMER = "medlemmer"
    }
}
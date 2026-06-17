package no.nav.sikkerhetstjenesten.entraproxy.felles.cache

import io.micrometer.core.instrument.MeterRegistry
import org.springframework.stereotype.Component
import java.util.Locale
import java.util.Locale.getDefault

@Component
class ValkeyCacheTeller(private val registry: MeterRegistry) {

    fun tell(operasjon: Operasjon, cache: String, resultat: Resultat, n: Int = 1) =
        registry.counter(
            "valkey.cache.operasjoner",
            "operasjon", operasjon.name.lowercase(getDefault()),
            "cache", cache,
            "resultat", resultat.name.lowercase(getDefault())
        ).increment(n.toDouble())


    enum class Operasjon { GET_ONE, PUT_ONE, DELETE, CLEAR }
    enum class Resultat { HIT, MISS, OK, FEILET }
}
package no.nav.sikkerhetstjenesten.entraproxy.felles.rest

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.time.Duration

interface CachableRestConfig {
    val varighet: Duration get() = Duration.ofHours(3)
    val navn: String
}

@Component
class MedlemmerCachableRestConfig(@param:Value("\${medlemmer.varighet:17h}") override val varighet: Duration) : CachableRestConfig {
    override val navn = MEDLEMMER

    companion object {
        const val MEDLEMMER = "medlemmer"
    }
}
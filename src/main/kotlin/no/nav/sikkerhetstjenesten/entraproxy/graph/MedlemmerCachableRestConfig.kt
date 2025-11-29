package no.nav.sikkerhetstjenesten.entraproxy.graph

import no.nav.sikkerhetstjenesten.entraproxy.felles.rest.CachableRestConfig
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.time.Duration

@Component
class MedlemmerCachableRestConfig(@param:Value("\${medlemmer.varighet:3h}") override val varighet: Duration) :
    CachableRestConfig {
    override val navn = MEDLEMMER

    companion object {
        const val MEDLEMMER = "medlemmer"
    }
}
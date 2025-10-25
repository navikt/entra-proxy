package no.nav.sikkerhetstjenesten.entraproxy.norg

import no.nav.sikkerhetstjenesten.entraproxy.graph.Enhetsnummer
import org.springframework.stereotype.Service

@Service
class NorgTjeneste(private val a : NorgRestClientAdapter) {
    fun navnFor(enhetsnummer: Enhetsnummer) = a.navnFor(enhetsnummer.verdi)
}
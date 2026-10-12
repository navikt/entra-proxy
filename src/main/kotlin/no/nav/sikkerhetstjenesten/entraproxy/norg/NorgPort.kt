package no.nav.sikkerhetstjenesten.entraproxy.norg

import no.nav.sikkerhetstjenesten.entraproxy.graph.Enhet.Enhetnummer

interface NorgPort {
    fun navnFor(enhet: Enhetnummer): String
}

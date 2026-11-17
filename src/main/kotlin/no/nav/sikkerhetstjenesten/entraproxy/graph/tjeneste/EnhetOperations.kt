package no.nav.sikkerhetstjenesten.entraproxy.graph.tjeneste

import no.nav.sikkerhetstjenesten.entraproxy.graph.Enhet.Enhetnummer

interface EnhetOperations {
    fun navn(enhetnummer: Enhetnummer): String
}

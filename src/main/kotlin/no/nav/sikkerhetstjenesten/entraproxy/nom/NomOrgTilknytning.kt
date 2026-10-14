package no.nav.sikkerhetstjenesten.entraproxy.nom

import com.fasterxml.jackson.annotation.JsonValue
import no.nav.sikkerhetstjenesten.entraproxy.graph.AnsattId
import no.nav.sikkerhetstjenesten.entraproxy.graph.Enhet.Enhetnummer

data class NomOrgTilknytning(val orgEnhet: NomEnhet, val erDagligOppfolging: Boolean = false)  {
    data class NomEnhet(
        val id: NomIdent,
        val navn: String,
        val tilgangsenhetId: Enhetnummer? = null,
        val ledere: List<NomLeder> = emptyList()
    ) {
        data class NomLeder(val ressurs: NomLederRessurs) {
            data class NomLederRessurs(val navident: AnsattId, val visningsnavn: String)
        }

        data class NomIdent(@JsonValue val verdi: String) : Comparable<NomIdent> {

            init {
                require(NOM_IDENT_REGEX.matches(verdi)) {
                    "Ugyldig NomIdent '$verdi', forventet $NOM_IDENT_LENGTH tegn: 2 små bokstaver, 3 siffer og en liten bokstav"
                }
            }

            override fun compareTo(other: NomIdent): Int = verdi.compareTo(other.verdi)

            override fun toString() = verdi

            companion object {
                private const val NOM_IDENT_LENGTH = 6
                private val NOM_IDENT_REGEX = Regex("^[a-z]{2}[0-9]{3}[a-z]$")
            }
        }
    }
}
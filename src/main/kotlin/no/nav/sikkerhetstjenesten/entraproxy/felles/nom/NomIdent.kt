package no.nav.sikkerhetstjenesten.entraproxy.felles.nom

import com.fasterxml.jackson.annotation.JsonValue

data class NomIdent(@JsonValue val verdi: String) : Comparable<NomIdent> {

    init {
        require(NOM_IDENT_REGEX.matches(verdi)) {
            "Ugyldig NomIdent '$verdi', forventet $NOM_IDENT_LENGTH tegn: 2 små bokstaver, 3 siffer og en liten bokstav"
        }
    }

    override fun compareTo(other: NomIdent): Int = verdi.compareTo(other.verdi)

    override fun toString() = verdi

    companion object {
        const val NOM_IDENT_LENGTH = 6
        private val NOM_IDENT_REGEX = Regex("^[a-z]{2}[0-9]{3}[a-z]$")
    }
}

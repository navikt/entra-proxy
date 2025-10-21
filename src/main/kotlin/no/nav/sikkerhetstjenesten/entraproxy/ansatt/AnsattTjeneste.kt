package no.nav.sikkerhetstjenesten.entraproxy.ansatt

import io.micrometer.core.annotation.Timed
import io.micrometer.core.instrument.Tags
import no.nav.sikkerhetstjenesten.entraproxy.ansatt.Ansatt
import no.nav.sikkerhetstjenesten.entraproxy.ansatt.AnsattId
import no.nav.boot.conditionals.ConditionalOnGCP
import org.springframework.stereotype.Service

@Service
@Timed
@ConditionalOnGCP
class AnsattTjeneste(private val resolver: AnsattGruppeResolver) {



    fun ansatt(ansattId: AnsattId) =
        Ansatt(ansattId, ansattGrupper(ansattId)).also {
        }

    private fun ansattGrupper(ansattId: AnsattId) = resolver.grupperForAnsatt(ansattId)


    companion object {
        private const val MEDLEM = "medlem"
    }
}






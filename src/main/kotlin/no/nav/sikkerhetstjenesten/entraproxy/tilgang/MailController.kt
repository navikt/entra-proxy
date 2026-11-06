package no.nav.sikkerhetstjenesten.entraproxy.tilgang

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import no.nav.sikkerhetstjenesten.entraproxy.graph.EntraMailTjeneste
import no.nav.sikkerhetstjenesten.felles.notifikasjon.MailRequest
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Tag(name = "MailController", description = "Disse endepunktene skal brukes i produksjon, men normalt ikke kalles dirkte")
@RestController
@RequestMapping(API_V1)
class MailController(private val mail: EntraMailTjeneste) {

    @PostMapping("mail")
    @Operation(summary = "Send e-post")
    fun sendMail(@RequestBody message: MailRequest) =
        mail.send(message)
}

package no.nav.sikkerhetstjenesten.entraproxy.tilgang

import no.nav.sikkerhetstjenesten.entraproxy.tilgang.EntraController.Companion.API_V1
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping

@Controller
@RequestMapping("$API_V1/utforsker")
class UtforskerController {

    @GetMapping("medlemmer")
    fun visMedlemmer() = "redirect:/medlemmer.html"

    @GetMapping("ansatt")
    fun ansatt() = "redirect:/ansatt.html"
}

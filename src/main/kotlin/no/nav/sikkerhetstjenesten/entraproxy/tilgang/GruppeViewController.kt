package no.nav.sikkerhetstjenesten.entraproxy.tilgang

import no.nav.sikkerhetstjenesten.entraproxy.tilgang.EntraController.Companion.API_V1
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping

@Controller
@RequestMapping("$API_V1/gruppe")
class GruppeViewController {

    @GetMapping("vis")
    fun visMedlemmer() = "redirect:/gruppe/medlemmer.html"
}

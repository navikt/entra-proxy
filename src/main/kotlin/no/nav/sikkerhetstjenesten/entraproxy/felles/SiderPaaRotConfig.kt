package no.nav.sikkerhetstjenesten.entraproxy.felles

import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

/** Gjør sidene under /html også tilgjengelige direkte på rot, f.eks. /ansatt.html. */
@Configuration
class SiderPaaRotConfig : WebMvcConfigurer {

    override fun addViewControllers(registry: ViewControllerRegistry) {
        SIDER.forEach { side ->
            registry.addViewController("/$side.html").setViewName("forward:/html/$side.html")
        }
    }

    companion object {
        val SIDER = listOf("ansatt", "grupper", "gruppematrise", "logout", "innlogging-feilet", "mangler-tilgang")
    }
}

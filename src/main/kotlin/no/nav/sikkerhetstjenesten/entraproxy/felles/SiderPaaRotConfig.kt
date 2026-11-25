package no.nav.sikkerhetstjenesten.entraproxy.felles

import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
class SiderPaaRotConfig : WebMvcConfigurer {

    override fun addViewControllers(registry: ViewControllerRegistry) {
        SIDER.forEach { side ->
            registry.addViewController("/$side.html").setViewName("forward:/html/$side.html")
        }
    }

    private companion object {
        private val SIDER = listOf("ansatt", "grupper", "gruppematrise", "logout", "401", "403")
    }
}

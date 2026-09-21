package no.nav.sikkerhetstjenesten.entraproxy.felles.leder

import org.springframework.context.ApplicationEvent

class LederHendelse(source: Any, val leder: String) : ApplicationEvent(source)
package no.nav.sikkerhetstjenesten.entraproxy.felles.utils

import org.springframework.context.ApplicationEvent

class NyLederHendelse(source: Any, val leder: String) : ApplicationEvent(source)
package no.nav.sikkerhetstjenesten.entraproxy.felles.utils

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class Auditor {
    private val secureLog = LoggerFactory.getLogger("secureLog")
    fun trace(message: String) = secureLog.trace(message)
    fun warn(message: String) = secureLog.warn(message)
    fun info(message: String) = secureLog.info(message)

}
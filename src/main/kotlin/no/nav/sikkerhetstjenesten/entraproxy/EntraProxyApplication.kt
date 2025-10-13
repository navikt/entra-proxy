package no.nav.sikkerhetstjenesten.entraproxy

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class EntraProxyApplication

fun main(args: Array<String>) {
    runApplication<EntraProxyApplication>(*args)
}

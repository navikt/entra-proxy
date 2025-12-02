package no.nav.sikkerhetstjenesten

import no.nav.sikkerhetstjenesten.entraproxy.felles.utils.cluster.ClusterUtils.Companion.profiler
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

class TestApp {
    @SpringBootApplication
    class TestApp

    fun main(args: Array<String>) {
        runApplication<no.nav.sikkerhetstjenesten.TestApp>(*args) {
            setAdditionalProfiles(*profiler)
        }
    }
}
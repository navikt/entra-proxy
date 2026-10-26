package no.nav.sikkerhetstjenesten.entraproxy.felles

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.mockk
import no.nav.sikkerhetstjenesten.felles.rest.DownstreamUriCapturingInterceptor
import org.springframework.beans.factory.support.DefaultListableBeanFactory
import org.springframework.http.client.ClientHttpRequestInterceptor
import org.springframework.security.oauth2.client.web.client.OAuth2ClientHttpRequestInterceptor
import org.zalando.logbook.spring.LogbookClientHttpRequestInterceptor

class CommonHttpClientInterceptorsTest : BehaviorSpec({
    Given("felles HTTP-interceptorer") {
        listOf(false, true).forEach { withLogbook ->
            When("klienten konfigureres med logbook=$withLogbook") {
                Then("ligger URI-fangst først og eksisterende interceptorer beholder rekkefølgen") {
                    val beanFactory = DefaultListableBeanFactory()
                    val logbook = mockk<LogbookClientHttpRequestInterceptor>()
                    if (withLogbook) {
                        beanFactory.registerSingleton("logbook", logbook)
                    }
                    val common = FellesHttpClientInterceptors(
                        beanFactory.getBeanProvider(LogbookClientHttpRequestInterceptor::class.java)
                    )
                    val oauth = mockk<OAuth2ClientHttpRequestInterceptor>()
                    val existing = mockk<ClientHttpRequestInterceptor>()
                    val interceptors = mutableListOf<ClientHttpRequestInterceptor>(oauth, existing)

                    common.configure(interceptors)

                    interceptors.first().shouldBeInstanceOf<DownstreamUriCapturingInterceptor>()
                    interceptors.drop(1) shouldBe
                        listOf(oauth, existing) + if (withLogbook) listOf(logbook) else emptyList()
                }
            }
        }
    }
})

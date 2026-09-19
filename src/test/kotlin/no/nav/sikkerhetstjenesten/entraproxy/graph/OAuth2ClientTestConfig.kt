package no.nav.sikkerhetstjenesten.entraproxy.graph

import no.nav.sikkerhetstjenesten.entraproxy.felles.rest.DefaultRestErrorHandler
import org.springframework.beans.factory.ObjectProvider
import org.springframework.boot.restclient.RestClientCustomizer
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.core.env.Environment
import org.springframework.http.HttpStatusCode
import org.springframework.web.client.support.RestClientHttpServiceGroupConfigurer
import org.zalando.logbook.Logbook
import org.zalando.logbook.spring.LogbookClientHttpRequestInterceptor
import tools.jackson.databind.json.JsonMapper

@TestConfiguration
class OAuth2ClientTestConfig {

    @Bean
    fun restClientCustomizer() =
        RestClientCustomizer { c ->
            c.requestInterceptors { it.add(LogbookClientHttpRequestInterceptor(Logbook.builder().build())) }
            c.defaultStatusHandler(HttpStatusCode::isError, DefaultRestErrorHandler()::handle)
        }

    @Bean
    fun jsonMapper() =
        JsonMapper.builder()
            .findAndAddModules()
            .build()
    @Bean
    fun restClientGroupCustomizer(
        customizers: ObjectProvider<RestClientCustomizer>, env: Environment) =
        RestClientHttpServiceGroupConfigurer { groups ->
            groups.forEachClient { group, builder ->
                env.getRequiredProperty("${SERVICE_CLIENT_PREFIX}.${group.name()}.base-url").let(builder::baseUrl)
                customizers.forEach { it.customize(builder) }
            }
        }

    companion object {
        const val SERVICE_CLIENT_PREFIX = "spring.http.serviceclient"
    }
}
package no.nav.sikkerhetstjenesten.entraproxy.security

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Configuration
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = "spring.data.redis", name = ["host"])
@EnableRedisHttpSession
class ValkeySessionConfig

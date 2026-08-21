package no.nav.sikkerhetstjenesten.entraproxy.felles.rest


import io.lettuce.core.RedisCommandTimeoutException
import org.springframework.core.annotation.AliasFor
import org.springframework.dao.QueryTimeoutException
import org.springframework.resilience.annotation.Retryable
import org.springframework.stereotype.Service
import org.springframework.web.client.ResourceAccessException
import java.lang.annotation.Inherited
import java.net.SocketTimeoutException
import kotlin.annotation.AnnotationRetention.RUNTIME
import kotlin.annotation.AnnotationTarget.CLASS
import kotlin.annotation.AnnotationTarget.FUNCTION
import kotlin.reflect.KClass

@Retryable(delayString = "\${retry.delay:1s}")
@Target(FUNCTION, CLASS)
@Retention(RUNTIME)
@Inherited
@MustBeDocumented
@Service
annotation class RetryingWhenRecoverableService(
    @get:AliasFor(annotation = Retryable::class) val value: Array<KClass<out Throwable>> = [RecoverableRestException::class, SocketTimeoutException::class, ResourceAccessException::class, RedisCommandTimeoutException::class, QueryTimeoutException::class]
)
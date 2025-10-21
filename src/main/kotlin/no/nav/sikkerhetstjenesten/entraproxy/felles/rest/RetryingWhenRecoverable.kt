package no.nav.sikkerhetstjenesten.entraproxy.felles.rest


import no.nav.sikkerhetstjenesten.entraproxy.felles.rest.LoggingRetryListener.Companion.LOGGING_RETRY_LISTENER
import org.springframework.core.annotation.AliasFor
import org.springframework.retry.annotation.Backoff
import org.springframework.retry.annotation.Retryable
import org.springframework.web.client.ResourceAccessException
import java.lang.annotation.Inherited
import kotlin.reflect.KClass

@Retryable
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@Inherited
@MustBeDocumented
annotation class RetryingWhenRecoverable(
    @get:AliasFor(annotation = Retryable::class) val value: Array<KClass<out Throwable>> = [RecoverableRestException::class, ResourceAccessException::class],
    @get:AliasFor(annotation = Retryable::class) val maxAttempts: Int = 3,
    @get:AliasFor(annotation = Retryable::class) val listeners: Array<String> = [LOGGING_RETRY_LISTENER],
    @get:AliasFor(annotation = Retryable::class) val backoff: Backoff = Backoff(delay = 1000))
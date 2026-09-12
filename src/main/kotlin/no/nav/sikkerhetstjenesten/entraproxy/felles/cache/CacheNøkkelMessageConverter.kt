package no.nav.sikkerhetstjenesten.entraproxy.felles.cache

import org.springframework.http.MediaType
import org.springframework.messaging.Message
import org.springframework.messaging.converter.AbstractMessageConverter

class CacheNøkkelMessageConverter : AbstractMessageConverter(MediaType.APPLICATION_OCTET_STREAM) {
    override fun supports(clazz: Class<*>) =
        clazz == CacheNøkkel::class.java

    override fun convertFromInternal(message: Message<*>, targetClass: Class<*>, conversionHint: Any?) =
        (message.payload as? ByteArray)
            ?.takeIf { targetClass == CacheNøkkel::class.java }
            ?.toString(Charsets.UTF_8)
            ?.let(::CacheNøkkel)
}
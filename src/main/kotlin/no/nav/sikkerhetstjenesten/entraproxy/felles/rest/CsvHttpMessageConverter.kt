package no.nav.sikkerhetstjenesten.entraproxy.felles.rest

import no.nav.sikkerhetstjenesten.entraproxy.graph.AnsattBasis
import org.springframework.http.HttpInputMessage
import org.springframework.http.HttpOutputMessage
import org.springframework.http.MediaType
import org.springframework.http.converter.AbstractGenericHttpMessageConverter
import org.springframework.http.converter.HttpMessageNotReadableException
import java.io.OutputStreamWriter
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import java.nio.charset.StandardCharsets.UTF_8

class CsvHttpMessageConverter : AbstractGenericHttpMessageConverter<Any>(MediaType("text", "csv", UTF_8)) {

    override fun canRead(clazz: Class<*>, mediaType: MediaType?) = false

    override fun canWrite(clazz: Class<*>, mediaType: MediaType?) = false

    override fun getSupportedMediaTypes(clazz: Class<*>) = supportedMediaTypes

    override fun canWrite(type: Type?, clazz: Class<*>, mediaType: MediaType?) =
        supportsMediaType(mediaType) && type.ansattBasisElementTypeOrNull() != null

    override fun readInternal(clazz: Class<out Any>, inputMessage: HttpInputMessage): Nothing =
        throw HttpMessageNotReadableException("CSV-lesing er ikke støttet", inputMessage)

    override fun read(type: Type, contextClass: Class<*>?, inputMessage: HttpInputMessage): Nothing =
        throw HttpMessageNotReadableException("CSV-lesing er ikke støttet", inputMessage)

    override fun writeInternal(value: Any, type: Type?, outputMessage: HttpOutputMessage) {
        @Suppress("UNCHECKED_CAST")
        val rader = value as? Collection<AnsattBasis> ?: emptyList()
        OutputStreamWriter(outputMessage.body, UTF_8).use {
            writer -> rader.forEach {
                writer.write(it.navIdent.verdi + NEWLINE)
            }
        }
    }

    private fun supportsMediaType(mediaType: MediaType?) =
        mediaType == null || supportedMediaTypes.any {
            it.isCompatibleWith(mediaType)
        }

    private fun Type?.ansattBasisElementTypeOrNull(): Type? {
        val parameterized = this as? ParameterizedType ?: return null
        if (!Collection::class.java.isAssignableFrom(parameterized.rawType as? Class<*> ?: return null)) return null
        val elementType = parameterized.actualTypeArguments.firstOrNull() as? Class<*> ?: return null
        return elementType.takeIf { AnsattBasis::class.java.isAssignableFrom(it) }
    }

    companion object {
        private const val NEWLINE = "\r\n"
        const val TEXT_CSV_VALUE = "text/csv;charset=UTF-8"
    }
}

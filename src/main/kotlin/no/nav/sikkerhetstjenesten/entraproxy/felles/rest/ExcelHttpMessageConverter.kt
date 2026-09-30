package no.nav.sikkerhetstjenesten.entraproxy.felles.rest

import no.nav.sikkerhetstjenesten.entraproxy.graph.AnsattBasis
import org.apache.poi.xssf.streaming.SXSSFWorkbook
import org.springframework.http.HttpInputMessage
import org.springframework.http.HttpOutputMessage
import org.springframework.http.MediaType
import org.springframework.http.converter.AbstractGenericHttpMessageConverter
import org.springframework.http.converter.HttpMessageNotReadableException
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type

/**
 * Skriver ut samlinger av [AnsattBasis] (Ansatt, UtvidetAnsatt, ...) som en Excel-arbeidsbok (.xlsx) når
 * klienten ber om `Accept: application/vnd.openxmlformats-officedocument.spreadsheetml.sheet`. JSON
 * forblir default for alle andre klienter.
 */
class ExcelHttpMessageConverter :
    AbstractGenericHttpMessageConverter<Any>(MediaType.parseMediaType(EXCEL_VALUE)) {

    override fun canRead(clazz: Class<*>, mediaType: MediaType?) = false

    override fun canWrite(clazz: Class<*>, mediaType: MediaType?) = false

    // Se tilsvarende kommentar i CsvHttpMessageConverter: uten denne overstyringen blir
    // Excel-typen aldri foreslått som produserbar type, siden vi kun skriver basert på generisk elementtype.
    override fun getSupportedMediaTypes(clazz: Class<*>) = supportedMediaTypes

    override fun canWrite(type: Type?, clazz: Class<*>, mediaType: MediaType?) =
        supportsMediaType(mediaType) && type.ansattBasisElementTypeOrNull() != null

    override fun readInternal(clazz: Class<out Any>, inputMessage: HttpInputMessage): Nothing =
        throw HttpMessageNotReadableException("Excel-lesing er ikke støttet", inputMessage)

    override fun read(type: Type, contextClass: Class<*>?, inputMessage: HttpInputMessage): Nothing =
        throw HttpMessageNotReadableException("Excel-lesing er ikke støttet", inputMessage)

    override fun writeInternal(value: Any, type: Type?, outputMessage: HttpOutputMessage) {
        @Suppress("UNCHECKED_CAST")
        val rader = value as? Collection<AnsattBasis> ?: emptyList()
        SXSSFWorkbook().use { workbook ->
            val ark = workbook.createSheet("Ansatte")
            ark.createRow(0).apply {
                KOLONNER.forEachIndexed { i, navn -> createCell(i).setCellValue(navn) }
            }
            rader.forEachIndexed { radIndeks, ansatt ->
                ark.createRow(radIndeks + 1).apply {
                    createCell(0).setCellValue(ansatt.navIdent.verdi)
                    createCell(1).setCellValue(ansatt.visningNavn)
                    createCell(2).setCellValue(ansatt.fornavn)
                    createCell(3).setCellValue(ansatt.etternavn)
                }
            }
            workbook.write(outputMessage.body)
        }
    }

    private fun supportsMediaType(mediaType: MediaType?) =
        mediaType == null || supportedMediaTypes.any { it.isCompatibleWith(mediaType) }

    private fun Type?.ansattBasisElementTypeOrNull(): Type? {
        val parameterized = this as? ParameterizedType ?: return null
        if (!Collection::class.java.isAssignableFrom(parameterized.rawType as? Class<*> ?: return null)) return null
        val elementType = parameterized.actualTypeArguments.firstOrNull() as? Class<*> ?: return null
        return elementType.takeIf { AnsattBasis::class.java.isAssignableFrom(it) }
    }

    companion object {
        private val KOLONNER = listOf("navIdent", "visningNavn", "fornavn", "etternavn")

        /** Brukes i `@GetMapping(produces = [...])` slik at Excel vises som et valgbart format i Swagger. */
        const val EXCEL_VALUE = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
    }
}

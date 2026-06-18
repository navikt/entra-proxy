package no.nav.sikkerhetstjenesten.entraproxy.felles.cache

data class CacheNøkkelConfig(val name: String, val extraPrefix: String? = null){

    val fullName: String get() = extraPrefix?.let { "$name:$it" } ?: name
    val prefix: String get() = "$name::"

    fun tilNøkkel(nøkkel: String): String {
        val extra = extraPrefix?.let { "$it:" } ?: ""
        return "$prefix$extra$nøkkel"
    }
}
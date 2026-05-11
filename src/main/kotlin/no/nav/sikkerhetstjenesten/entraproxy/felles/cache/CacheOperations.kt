package no.nav.sikkerhetstjenesten.entraproxy.felles.cache

interface CacheOperations {
    fun delete(cache: CachableConfig, id: String): Long
    fun tilNøkkel(cache: CachableConfig, id: String): String
}
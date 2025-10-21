package no.nav.sikkerhetstjenesten.entraproxy.ansatt

import io.swagger.v3.oas.annotations.media.Schema

// kan forenkles da avvinsing er ikke del av infoen, kny navn

enum class GruppeMetadata(val meta: AvvisningsKode, val begrunnelse: String, val kortNavn: String) {

    STRENGT_FORTROLIG(AvvisningsKode.AVVIST_STRENGT_FORTROLIG_ADRESSE, "Du har ikke tilgang til brukere med strengt fortrolig adresse", "Kode 6"),
    STRENGT_FORTROLIG_UTLAND(AvvisningsKode.AVVIST_STRENGT_FORTROLIG_UTLAND, "Du har ikke tilgang til brukere med strengt fortrolig adresse i utlandet", "Paragraf 19"),
    FORTROLIG(AvvisningsKode.AVVIST_FORTROLIG_ADRESSE, "Du har ikke tilgang til brukere med fortrolig adresse", "Kode 7"),
    SKJERMING(AvvisningsKode.AVVIST_SKJERMING, "Du har ikke tilgang til Nav-ansatte og andre skjermede brukere", "Skjerming"),
    NASJONAL(AvvisningsKode.AVVIST_GEOGRAFISK,"Du har ikke tilgang til brukerens geografiske område eller oppfølgingsenhet","Geografisk tilknytning"),
    UTENLANDSK(AvvisningsKode.AVVIST_PERSON_UTLAND, "Du har ikke tilgang til person bosatt i utlandet", "Person bosatt utland"),
    UKJENT_BOSTED(AvvisningsKode.AVVIST_UKJENT_BOSTED, "Du har ikke tilgang til person uten kjent adresse", "Person bosatt ukjent bosted")
}
@Schema(description = "Avvisningskoder")
enum class AvvisningsKode {
    AVVIST_STRENGT_FORTROLIG_ADRESSE,
    AVVIST_STRENGT_FORTROLIG_UTLAND,
    AVVIST_SKJERMING,
    AVVIST_FORTROLIG_ADRESSE,
    AVVIST_GEOGRAFISK,
    AVVIST_PERSON_UTLAND,
    AVVIST_UKJENT_BOSTED
}
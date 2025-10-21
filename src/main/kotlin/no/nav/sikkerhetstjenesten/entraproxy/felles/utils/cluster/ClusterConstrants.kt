package no.nav.sikkerhetstjenesten.entraproxy.felles.utils.cluster


internal object ClusterConstants {
    internal const val LOCAL = "local"
    internal const val GCP = "gcp"
    internal const val TEST = "test"
    internal const val DEV = "dev"
    internal const val PROD = "prod"
    internal const val DEV_GCP = "$DEV-$GCP"
    internal const val PROD_GCP = "$PROD-$GCP"
    internal const val NAIS_CLUSTER_NAME = "NAIS_CLUSTER_NAME"
}
package no.nav.sikkerhetstjenesten.entraproxy.graph

data class EntraMailRequest(val message: MailMessage, val saveToSentItems: Boolean) {
    data class MailMessage(
        val subject: String?,
        val body: MailBody,
        val toRecipients: List<MailRecipient>,
        val ccRecipients: List<MailRecipient>,
        val bccRecipients: List<MailRecipient>,
        val replyTo: List<MailRecipient>) {
        data class MailBody(val contentType: String, val content: String?)
        data class MailRecipient(val emailAddress: MailAddress) {
            data class MailAddress(val address: String)
        }
    }
}
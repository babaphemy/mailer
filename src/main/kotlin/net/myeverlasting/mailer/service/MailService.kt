package net.myeverlasting.mailer.service

import com.sendgrid.Content
import com.sendgrid.Email
import com.sendgrid.Mail
import com.sendgrid.Method
import com.sendgrid.Personalization
import com.sendgrid.Request
import com.sendgrid.Response
import com.sendgrid.SendGrid
import net.myeverlasting.mailer.model.EmailValidationResult
import net.snowflake.client.jdbc.internal.google.gson.Gson
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.io.IOException

@Service
class MailService {
    private val apiKey: String = "SG.g"
    private val validationKey = "2-p"
    private val log: Logger = LoggerFactory.getLogger(javaClass)

    fun postToSendGrid(mail: Mail): String {
        val sg = SendGrid(apiKey)
        val request = Request()
        val toValidate = mutableListOf<String>()
        return try {
            request.method = Method.POST
            request.endpoint = "mail/send"
            request.body = mail.build()
            val response: Response = sg.api(request)
            if (response.statusCode == 202) {
                getRecipientsToValidate(mail, toValidate)
                "SENT"
            } else {
                log.warn("not 202", response)
                "FAILED"
            }
        } catch (ex: IOException) {
            log.error("IO_ERROR", ex)
            "FAILED"
        } finally {
            if(toValidate.isNotEmpty()) validateRecipients(toValidate, mail.subject ?: "NOI")
        }
    }
    private fun getRecipientsToValidate(mail: Mail, toValidate: MutableList<String>){
        val personalizations = mail.personalization
        personalizations.forEach {personalization ->
            personalization.tos.forEach { to ->
                toValidate.add(to.email)
            }
        }

    }

    private fun validateRecipients(email: MutableList<String>, subject: String){
        val statuses = mutableListOf<String>()
        val initSendgrid = SendGrid(validationKey)
        email.forEach {recipient ->
            try {
                val requestBody = """
                {
                    "email": "$recipient",
                    "source": "NOI"
                }""".trimIndent()

                val request = Request().apply {
                    method = Method.POST
                    endpoint = "/validations/email"
                    body = requestBody
                }

                val response = initSendgrid.api(request)
                val readResponse = Gson().fromJson(response.body, EmailValidationResult::class.java)
                val responseStatus = readResponse.result.verdict
                if(!responseStatus.equals("valid", ignoreCase = true)) {
                    statuses.add("$recipient : $responseStatus")
                }
            } catch (e: IOException) {
                log.info(e.message)
                throw e
            }
        }
        sendStatusMail(statuses, subject)

    }
    private fun sendStatusMail(statuses: List<String>, mailSubject: String): String{
        val sg = SendGrid(apiKey)
        val from = Email("noiDonotreply@technipfmc.com", "NOI Admin")
        val to = Email("femi@myessl.com")
        val content = Content("TEXT_HTML", createHtmlContentWithList(statuses, mailSubject))
        val personalization = Personalization().apply {
            addTo(to)
        }
        val mail = Mail().apply {
            addContent(content)
            setFrom(from)
            subject = "Email Delivery Status: $mailSubject"
            addPersonalization(personalization)

        }

        val request = Request().apply {
            method = Method.POST
            endpoint = "mail/send"
            body = mail.build()
        }
        return try {
            val response: Response = sg.api(request)
            if (response.statusCode == 202) {
                "SENT"
            } else {
                "FAILED"
            }
        } catch (ex: IOException) {
            log.info("Failed when sending mail. Exception: {}$ex")
            "FAILED"
        }
    }
    private fun createHtmlContentWithList(listItems: List<String>, mailSubject: String): String {
        val listItemHtml = listItems.joinToString(separator = "") { "<li>$it</li>" }
        return "<html><body><h4>These email recipients may likely not receive your email: $mailSubject </h4>" +
                "<p>Kindly review the recipients. </p><br /></br /><ul>$listItemHtml</ul></body></html>"
    }

}
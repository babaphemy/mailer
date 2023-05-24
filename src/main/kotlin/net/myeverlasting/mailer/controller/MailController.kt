package net.myeverlasting.mailer.controller

import com.sendgrid.Content
import com.sendgrid.Email
import com.sendgrid.Mail
import com.sendgrid.Personalization
import net.myeverlasting.mailer.service.MailService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/mail")
class MailController(
    private val mailService: MailService
) {
    @GetMapping("send")
    fun sendMail(): String{
        val mail = Mail()
        mail.addContent(Content("text/html","Content"))
        mail.setSubject("This is a test")
        mail.setFrom(Email("webmaster@myeverlasting.net","ESSL Admin"))
        mail.setTemplateId("d-5c6b4bdc1e0046db8e767e1ba511caa5")
        val per = Personalization().apply {
            subject = "This is a test"
            addDynamicTemplateData("number", 100200)
            addTo(Email("accounts@myessl.com","Staff Memo"))

        }
        mail.addPersonalization(per)
        return mailService.postToSendGrid(mail)
    }
}
package net.myeverlasting.mailer

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class MailerApplication

fun main(args: Array<String>) {
    runApplication<MailerApplication>(*args)
}

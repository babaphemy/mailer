package net.myeverlasting.mailer.service

import com.sendgrid.Mail
import com.sendgrid.Method
import com.sendgrid.Request
import com.sendgrid.Response
import com.sendgrid.SendGrid
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*
import org.mockito.ArgumentMatchers
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations
import org.slf4j.Logger

internal class MailServiceTest {
    private lateinit var mailService: MailService

    @Mock
    private lateinit var sendGrid: SendGrid


    @Mock
    private lateinit var logger: Logger

    @BeforeEach
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        mailService.apiKey = "YOUR_API_KEY" // Set your own API key for testing
        mailService.validationKey = "YOUR_VALIDATION_KEY" // Set your own validation key for testing
        mailService.log = logger
    }

    @Test
    fun postToSendGrid_ValidResponse_ReturnsSent() {
        // Arrange
        val request = Request()
        request.method = Method.POST
        request.endpoint = "mail/send"
        request.body = "mailBody"

        val response = Response()
        response.statusCode = 202

        Mockito.`when`(sendGrid.api(ArgumentMatchers.any()<Request>())).thenReturn(response)

        val mailServiceSpy = spy(mailService)
        Mockito.doNothing().`when`(mailServiceSpy).getRecipientsToValidate(any<Mail>(), anyList())

        // Act
        val result = mailServiceSpy.postToSendGrid(Mail())

        // Assert
        assertEquals("SENT", result)
        verify (mailServiceSpy, times(1)).getRecipientsToValidate(any<Mail>(), anyList())
        verify(logger, never()).warn(anyString(), any<Response>())
        verify(logger, never()).error(anyString(), any<IOException>())
        verify(mailServiceSpy, times(1)).validateRecipients(anyList(), anyString())
    }

    @Test
    fun postToSendGrid_InvalidResponse_ReturnsFailed() {
        // Arrange
        val request = Request()
        request.method = Method.POST
        request.endpoint = "mail/send"
        request.body = "mailBody"

        val response = Response()
        response.statusCode = 400

        `when`(sendGrid.api(any<Request>())).thenReturn(response)

        val mailServiceSpy = spy(mailService)
        doNothing().`when`(mailServiceSpy).getRecipientsToValidate(any<Mail>(), anyList())

        // Act
        val result = mailServiceSpy.postToSendGrid(Mail())

        // Assert
        assertEquals("FAILED", result)
        verify(mailServiceSpy, never()).getRecipientsToValidate(any<Mail>(), anyList())
        verify(logger, times(1)).warn(anyString(), any<Response>())
        verify(logger, never()).error(anyString(), any<IOException>())
        verify(mailServiceSpy, never()).validateRecipients(anyList(), anyString())
    }

    @Test
    fun postToSendGrid_IOException_ReturnsFailed() {
        // Arrange
        val request = Request()
        request.method = Method.POST
        request.endpoint = "mail/send"
        request.body = "mailBody"

        val exception = IOException("Test exception")

        `when`(sendGrid.api(any<Request>())).thenThrow(exception)

        val mailServiceSpy = spy(mailService)
        doNothing().`when`(mailServiceSpy).getRecipientsToValidate(any<Mail>(), anyList())

        // Act
        val result = mailServiceSpy.postToSendGrid(Mail())

        // Assert
        assertEquals("FAILED", result)
        verify(mailServiceSpy, never()).getRecipientsToValidate(any<Mail>(), anyList())
        verify(logger, never()).warn(anyString(), any<Response>())
        verify(logger, times(1)).error(anyString(), any<IOException>())
        verify(mailServiceSpy, never()).validateRecipients(anyList(), anyString())
    }

    @Test
    fun validateRecipients_ValidEmails_NoStatuses() {
        // Arrange
        val emails = listOf("test1@example.com", "test2@example.com")
        val subject = "Test Subject"

        //`when`(userService.currentUser).thenReturn(User("test@example.com"))

        val mailServiceSpy = spy(mailService)
        doNothing().`when`(mailServiceSpy).sendStatusMail(anyList(), anyString())

        // Act
        mailServiceSpy.validateRecipients(emails.toMutableList(), subject)

        // Assert
        verify(sendGrid, times(2)).api(any<Request>())
        verify(logger, never()).info(anyString())
        verify(mailServiceSpy, times(1)).sendStatusMail(emptyList(), subject)
    }

    @Test
    fun validateRecipients_InvalidEmails_StatusesGenerated() {
        // Arrange
        val emails = listOf("test1@example.com", "test2@example.com")
        val subject = "Test Subject"

        `when`(userService.currentUser).thenReturn(User("test@example.com"))

        val requestCaptor = argumentCaptor<Request>()
        val response = Response()
        response.body = "{\"result\":{\"verdict\":\"invalid\"}}"

        `when`(sendGrid.api(requestCaptor.capture())).thenReturn(response)

        val mailServiceSpy = spy(mailService)
        doNothing().`when`(mailServiceSpy).sendStatusMail(anyList(), anyString())

        // Act
        mailServiceSpy.validateRecipients(emails.toMutableList(), subject)

        // Assert
        verify(sendGrid, times(2)).api(any<Request>())
        verify(logger, never()).info(anyString())
        verify(mailServiceSpy, times(1)).sendStatusMail(listOf("test1@example.com : invalid"), subject)

        // Additional assertion for the captured request
        val capturedRequest = requestCaptor.firstValue
        assertEquals(Method.POST, capturedRequest.method)
        assertEquals("/validations/email", capturedRequest.endpoint)
        assertEquals("{\"email\":\"test1@example.com\",\"source\":\"NOI\"}", capturedRequest.body)
    }

    @Test
    fun sendStatusMail_ValidResponse_ReturnsSent() {
        // Arrange
        val requestCaptor = argumentCaptor<Request>()
        val response = Response()
        response.statusCode = 202

        `when`(sendGrid.api(requestCaptor.capture())).thenReturn(response)

        // Act
        val result = mailService.sendStatusMail(emptyList(), "Test Subject")

        // Assert
        assertEquals("SENT", result)
        verify(sendGrid, times(1)).api(any<Request>())
        assertEquals(Method.POST, requestCaptor.firstValue.method)
        assertEquals("mail/send", requestCaptor.firstValue.endpoint)
        assertEquals("mailBody", requestCaptor.firstValue.body)
    }
}
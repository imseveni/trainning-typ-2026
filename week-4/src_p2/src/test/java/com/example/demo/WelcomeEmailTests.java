package com.example.demo;

import com.example.demo.messaging.UserRegisteredEvent;
import com.example.demo.messaging.WelcomeEmailConsumer;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class WelcomeEmailTests {
    private final JavaMailSender sender = mock(JavaMailSender.class);
    private final WelcomeEmailConsumer consumer = new WelcomeEmailConsumer(sender, "no-reply@student.local");
    private final UserRegisteredEvent event = new UserRegisteredEvent("event-1", 1L, "student", "a@example.com");

    @Test
    void consumerSendsWelcomeEmailToRegisteredAddress() {
        consumer.consume(event);

        var message = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(sender).send(message.capture());
        assertArrayEquals(new String[] {"a@example.com"}, message.getValue().getTo());
        assertEquals("no-reply@student.local", message.getValue().getFrom());
        assertTrue(message.getValue().getText().contains("student"));
    }

    @Test
    void smtpFailureIsPropagatedSoListenerCanRetry() {
        doThrow(new MailSendException("SMTP offline")).when(sender).send(any(SimpleMailMessage.class));
        assertThrows(MailSendException.class, () -> consumer.consume(event));
    }
}

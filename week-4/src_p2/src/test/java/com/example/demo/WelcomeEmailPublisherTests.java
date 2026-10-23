package com.example.demo;

import com.example.demo.config.RabbitMqConfig;
import com.example.demo.messaging.UserRegisteredEvent;
import com.example.demo.messaging.WelcomeEmailPublisher;
import com.example.demo.model.AppUser;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class WelcomeEmailPublisherTests {
    @Test
    void publishesPersistentEventAndAcceptsBrokerConfirmation() {
        RabbitTemplate template = mock(RabbitTemplate.class);
        doAnswer(call -> {
            UserRegisteredEvent event = call.getArgument(2);
            MessagePostProcessor processor = call.getArgument(3);
            Message message = processor.postProcessMessage(new Message(new byte[0]));
            assertEquals("student@example.com", event.email());
            assertEquals(1L, event.userId());
            assertEquals(event.eventId(), message.getMessageProperties().getMessageId());
            assertEquals(MessageDeliveryMode.PERSISTENT, message.getMessageProperties().getDeliveryMode());
            CorrelationData correlation = call.getArgument(4);
            correlation.getFuture().complete(new CorrelationData.Confirm(true, null));
            return null;
        }).when(template).convertAndSend(eq(RabbitMqConfig.EXCHANGE), eq(RabbitMqConfig.ROUTING_KEY),
                any(Object.class), any(MessagePostProcessor.class), any(CorrelationData.class));

        assertDoesNotThrow(() -> new WelcomeEmailPublisher(template).publish(user()));
    }

    @Test
    void rejectsNackAndUnroutableMessages() {
        for (boolean returned : new boolean[] {false, true}) {
            RabbitTemplate template = mock(RabbitTemplate.class);
            doAnswer(call -> {
                CorrelationData correlation = call.getArgument(4);
                if (returned) {
                    correlation.setReturned(new ReturnedMessage(new Message(new byte[0]),
                            312, "NO_ROUTE", RabbitMqConfig.EXCHANGE, RabbitMqConfig.ROUTING_KEY));
                }
                correlation.getFuture().complete(new CorrelationData.Confirm(returned, null));
                return null;
            }).when(template).convertAndSend(anyString(), anyString(), any(Object.class),
                    any(MessagePostProcessor.class), any(CorrelationData.class));

            assertThrows(AmqpException.class, () -> new WelcomeEmailPublisher(template).publish(user()));
        }
    }

    private AppUser user() {
        AppUser user = new AppUser();
        user.setId(1L);
        user.setUsername("student");
        user.setEmail("student@example.com");
        return user;
    }
}

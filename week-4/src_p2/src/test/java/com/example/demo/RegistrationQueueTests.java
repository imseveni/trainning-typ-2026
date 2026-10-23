package com.example.demo;

import com.example.demo.dto.RegisterRequest;
import com.example.demo.messaging.WelcomeEmailPublisher;
import com.example.demo.model.AppUser;
import com.example.demo.repository.UserRepository;
import com.example.demo.service.AuthService;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.AmqpException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest
class RegistrationQueueTests {
    @Autowired AuthService authService;
    @Autowired UserRepository users;
    @Autowired Validator validator;
    @MockitoBean WelcomeEmailPublisher publisher;

    @Test
    void registrationPersistsEmailAndQueuesWelcomeEvent() {
        var response = authService.register(new RegisterRequest(
                "queue-student", "password123", "student@example.com"));

        assertNotNull(response.accessToken());
        AppUser saved = users.findByUsername("queue-student").orElseThrow();
        assertEquals("student@example.com", saved.getEmail());
        verify(publisher).publish(argThat(user -> saved.getId().equals(user.getId())
                && "student@example.com".equals(user.getEmail())));
    }

    @Test
    void brokerFailureRollsBackRegistration() {
        doThrow(new AmqpException("broker offline")).when(publisher).publish(any());

        assertThrows(AmqpException.class, () -> authService.register(new RegisterRequest(
                "broker-offline", "password123", "student@example.com")));

        assertFalse(users.existsByUsername("broker-offline"));
    }

    @Test
    void duplicateUsernameDoesNotPublishAnotherEvent() {
        authService.register(new RegisterRequest("duplicate", "password123", "a@example.com"));
        clearInvocations(publisher);

        assertThrows(IllegalArgumentException.class, () -> authService.register(
                new RegisterRequest("duplicate", "password123", "b@example.com")));
        verifyNoInteractions(publisher);
    }

    @Test
    void emailMustBePresentAndValid() {
        for (String email : new String[] {null, "", "invalid-address"}) {
            assertFalse(validator.validate(new RegisterRequest("student", "password123", email)).isEmpty());
        }
        assertTrue(validator.validate(new RegisterRequest("student", "password123", "a@example.com")).isEmpty());
    }
}

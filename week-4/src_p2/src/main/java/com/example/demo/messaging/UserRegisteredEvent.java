package com.example.demo.messaging;

// Định nghĩa nội dung message
public record UserRegisteredEvent(String eventId, Long userId, String username, String email) {
}

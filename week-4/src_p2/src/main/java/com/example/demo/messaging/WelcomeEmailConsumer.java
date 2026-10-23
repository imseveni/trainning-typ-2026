package com.example.demo.messaging;

import com.example.demo.config.RabbitMqConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

// Nhận message và gửi email
@Component
public class WelcomeEmailConsumer {

    // Ghi thông tin xử lý ra log
    private static final Logger log = LoggerFactory.getLogger(WelcomeEmailConsumer.class);

    // Công cụ gửi email qua SMTP
    private final JavaMailSender mailSender;

    // Địa chỉ người gửi
    private final String from;

    public WelcomeEmailConsumer(JavaMailSender mailSender,
            @Value("${app.mail.from}") String from) {

        this.mailSender = mailSender;
        this.from = from;
    }

    // Đăng ký nhận message từ queue
    // Khi listener nhận được message từ queue email.welcome,
    // hãy gọi method consume() để xử lý.
    @RabbitListener(queues = RabbitMqConfig.QUEUE)
    public void consume(UserRegisteredEvent event) {

        // Tạo một email văn bản thuần
        SimpleMailMessage message = new SimpleMailMessage();

        // Thêm người gửi
        message.setFrom(from);

        // Thêm người nhận
        message.setTo(event.email());

        // Thêm tiêu đề
        message.setSubject("Welcome to Student API!");

        // Thêm nội dung
        message.setText("Xin chào " + event.username()
                + ",\n\nBạn đã đăng ký tài khoản thành công. Chào mừng bạn đến với Student API!");

        // Gửi email qua SMTP
        mailSender.send(message);

        // Ghi log sau khi gửi thành công
        log.info("Welcome email sent: eventId={}, userId={}", event.eventId(), event.userId());
    }
}

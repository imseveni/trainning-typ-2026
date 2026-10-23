package com.example.demo.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// Tạo exchange, queue và đường đi của message
@Configuration
public class RabbitMqConfig {

    // Exchange nhận các event liên quan đến user
    public static final String EXCHANGE = "user.events";

    // Nhãn cho event user vừa đăng ký
    public static final String ROUTING_KEY = "user.registered";

    // Queue chứa công việc gửi email welcome
    public static final String QUEUE = "email.welcome";

    // Exchange nhận message bị chuyển khỏi queue chính khi lỗi
    public static final String DEAD_EXCHANGE = "email.dead";

    // Queue giữ các message lỗi để kiểm tra
    public static final String DEAD_QUEUE = "email.welcome.dead";

    @Bean
    Declarables welcomeEmailTopology() {

        // Tạo direct exchange chính.
        DirectExchange exchange = new DirectExchange(EXCHANGE);

        // Tạo direct exchange nhận message lỗi.
        DirectExchange deadExchange = new DirectExchange(DEAD_EXCHANGE);

        // Tạo queue chính.
        Queue queue = QueueBuilder.durable(QUEUE)
                .deadLetterExchange(DEAD_EXCHANGE)
                .deadLetterRoutingKey(DEAD_QUEUE).build();

        // Tạo queue nhận message lỗi.
        Queue deadQueue = QueueBuilder.durable(DEAD_QUEUE).build();

        // Tạo binding chính.
        Binding binding = BindingBuilder.bind(queue).to(exchange).with(ROUTING_KEY);

        // Tạo binding nhận message lỗi.
        Binding deadBinding = BindingBuilder.bind(deadQueue).to(deadExchange).with(DEAD_QUEUE);

        return new Declarables(
                exchange,
                deadExchange,
                queue,
                deadQueue,
                binding,
                deadBinding);
    }

    @Bean
    MessageConverter rabbitMessageConverter() {

        return new JacksonJsonMessageConverter("com.example.demo.messaging");
    }
}

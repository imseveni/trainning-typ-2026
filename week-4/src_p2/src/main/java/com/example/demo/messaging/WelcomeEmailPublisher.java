package com.example.demo.messaging;

import com.example.demo.config.RabbitMqConfig;
import com.example.demo.model.AppUser;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

// Producer gửi message
@Component
public class WelcomeEmailPublisher {

    // Công cụ Spring cung cấp để thao tác với RabbitMQ
    private final RabbitTemplate rabbitTemplate;

    public WelcomeEmailPublisher(RabbitTemplate rabbitTemplate) {

        this.rabbitTemplate = rabbitTemplate;
    }

    public void publish(AppUser user) {

        // Mỗi lần publish tạo một mã UUID nhận diện event mới
        String eventId = UUID.randomUUID().toString();

        // Tạo dữ liệu cần gửi
        UserRegisteredEvent event = new UserRegisteredEvent(
                eventId,
                user.getId(),
                user.getUsername(),
                user.getEmail());

        // Khi producer gửi message, RabbitMQ có thể phản hồi xác nhận sau đó.
        // CorrelationData giúp liên kết việc gửi này với phản hồi tương ứng.
        CorrelationData correlation = new CorrelationData(eventId);

        // convert: chuyển object Java thành message nhờ converter
        // send: gửi message đến RabbitMQ
        rabbitTemplate.convertAndSend(

                // Nơi producer gửi message vào
                RabbitMqConfig.EXCHANGE,

                // Nhãn dùng để định tuyến
                RabbitMqConfig.ROUTING_KEY,

                // Nội dung cần gửi
                event,

                // Bổ sung thuộc tính trước khi gửi
                message -> {

                    message.getMessageProperties()
                            .setMessageId(eventId);

                    message.getMessageProperties()
                            .setDeliveryMode(MessageDeliveryMode.PERSISTENT);

                    return message;

                },

                // Theo dõi broker confirm
                correlation);

        try {

            // Chờ RabbitMQ xác nhận
            CorrelationData.Confirm confirm = correlation
                    // Lấy đối tượng đại diện cho kết quả xác nhận sẽ có trong tương lai.
                    .getFuture()
                    // Chờ kết quả đó tối đa 5 giây.
                    .get(5, TimeUnit.SECONDS);

            if (!confirm.ack() || correlation.getReturned() != null) {

                throw new AmqpException("Welcome email event was not routed to the queue");
            }

        } catch (InterruptedException e) {

            // Đặt lại trạng thái bị ngắt
            // để phần xử lý phía trên vẫn nhận biết tín hiệu này.
            Thread.currentThread().interrupt();

            throw new AmqpException("Interrupted while publishing welcome email", e);

        } catch (ExecutionException | TimeoutException e) {

            throw new AmqpException("Cannot confirm welcome email publication", e);
        }
    }
}

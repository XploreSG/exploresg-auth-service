package com.exploresg.authservice.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ configuration for user events messaging.
 * This configuration sets up exchanges, queues, and bindings for sending
 * user creation events to the notification service.
 */
@Configuration
public class RabbitMQConfig {

    @Value("${rabbitmq.exchange.user-events}")
    private String userEventsExchange;

    @Value("${rabbitmq.queue.user-created}")
    private String userCreatedQueue;

    @Value("${rabbitmq.routing-key.user-created}")
    private String userCreatedRoutingKey;

    /**
     * Exchange for user-related events
     */
    @Bean
    public TopicExchange userEventsExchange() {
        return new TopicExchange(userEventsExchange);
    }

    /**
     * Queue for user created events (consumed by notification service)
     * - Durable: survives RabbitMQ restarts
     * - TTL: 24 hours (86400000 ms) - messages expire if not consumed
     * - DLX: Dead letter exchange for failed/expired messages
     */
    @Bean
    public Queue userCreatedQueue() {
        return QueueBuilder.durable(userCreatedQueue)
                .withArgument("x-dead-letter-exchange", userEventsExchange + ".dlx")
                .withArgument("x-message-ttl", 86400000) // 24 hours in milliseconds
                .build();
    }

    /**
     * Binding between exchange and queue
     */
    @Bean
    public Binding userCreatedBinding(Queue userCreatedQueue, TopicExchange userEventsExchange) {
        return BindingBuilder
                .bind(userCreatedQueue)
                .to(userEventsExchange)
                .with(userCreatedRoutingKey);
    }

    /**
     * Message converter to serialize/deserialize messages as JSON
     */
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    /**
     * RabbitTemplate with JSON message converter
     */
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(jsonMessageConverter());
        return rabbitTemplate;
    }
}

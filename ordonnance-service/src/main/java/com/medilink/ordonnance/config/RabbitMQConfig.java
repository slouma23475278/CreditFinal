package com.medilink.ordonnance.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String ORDONNANCE_EXCHANGE = "ordonnance.exchange";
    public static final String ORDONNANCE_QUEUE    = "ordonnance.notification.queue";
    public static final String ROUTING_KEY         = "ordonnance.notification";

    @Bean
    public TopicExchange ordonnanceExchange() {
        return new TopicExchange(ORDONNANCE_EXCHANGE);
    }

    @Bean
    public Queue ordonnanceQueue() {
        return QueueBuilder.durable(ORDONNANCE_QUEUE).build();
    }

    @Bean
    public Binding binding(Queue ordonnanceQueue, TopicExchange ordonnanceExchange) {
        return BindingBuilder.bind(ordonnanceQueue)
                .to(ordonnanceExchange)
                .with(ROUTING_KEY);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(jsonMessageConverter());
        return rabbitTemplate;
    }
}

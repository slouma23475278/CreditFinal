package com.medilink.ordonnance.messaging;

import com.medilink.ordonnance.config.RabbitMQConfig;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OrdonnanceEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(OrdonnanceEventPublisher.class);
    private final RabbitTemplate rabbitTemplate;

    public void publishEvent(OrdonnanceEvent event) {
        log.info("[RabbitMQ] Publishing event: type={}, ordonnanceId={}",
                event.getEventType(), event.getOrdonnanceId());
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.ORDONNANCE_EXCHANGE,
                RabbitMQConfig.ROUTING_KEY,
                event
        );
    }
}

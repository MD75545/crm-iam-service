package com.crm.kafka;

import com.crm.event.UserCreatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class KafkaProducerService {

    private static final Logger log = LoggerFactory.getLogger(KafkaProducerService.class);

    public static final String TOPIC_USER_CREATED = "user.created";

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    public void publishUserCreated(UserCreatedEvent event) {
        // Use userId as the message key so all events for the same user go
        // to the same partition (ordering preserved per user).
        String key = event.getUserId().toString();

        kafkaTemplate.send(TOPIC_USER_CREATED, key, event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish user.created event for userId={}", key, ex);
                    } else {
                        log.info("Published user.created event: userId={} partition={} offset={}",
                                key,
                                result.getRecordMetadata().partition(),
                                result.getRecordMetadata().offset());
                    }
                });
    }
}
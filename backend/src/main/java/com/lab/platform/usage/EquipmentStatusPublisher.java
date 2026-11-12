package com.lab.platform.usage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
public class EquipmentStatusPublisher {

    private static final Logger logger = LoggerFactory.getLogger(EquipmentStatusPublisher.class);
    private final SimpMessagingTemplate messagingTemplate;

    public EquipmentStatusPublisher(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void publishStatusUpdate(EquipmentStatusUpdateMessage message) {
        try {
            messagingTemplate.convertAndSend("/topic/equipment-status", message);
            logger.info("Broadcasted live equipment status update for '{}' (ID: {}): changeType={}",
                    message.getEquipmentName(), message.getEquipmentId(), message.getChangeType());
        } catch (Exception e) {
            logger.error("Failed to broadcast WebSocket status update for equipment {}: {}",
                    message.getEquipmentId(), e.getMessage());
        }
    }
}

package com.example.bang.config;

import com.example.bang.dto.RoomMessage;
import com.example.bang.model.Room;
import com.example.bang.service.RoomService;
import lombok.RequiredArgsConstructor;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketEventListener {

    private final RoomService roomService;
    private final SimpMessagingTemplate messagingTemplate;

    @EventListener
    public void handleWebSocketConnectListener(SessionConnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();
        log.info("New WebSocket connection: {}", sessionId);
    }

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();
        
        // Get room ID before leaving
        String roomId = roomService.getRoomIdForSession(sessionId);
        
        // Always keep player in room on disconnect to allow reconnect/refresh
        // Ghost players can be kicked by the host
        if (roomId != null) {
            log.info("Player disconnected from room {}. Marking session as inactive.", roomId);
            roomService.handleDisconnect(sessionId);
        }
        
        log.info("WebSocket disconnected: {}", sessionId);
    }
}

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
        
        if (roomId != null) {
            Optional<Room> roomOpt = roomService.getRoom(roomId);
            if (roomOpt.isPresent() && roomOpt.get().isGameStarted()) {
                log.info("Player disconnected from active game in room {}. Keeping player in room.", roomId);
                roomService.handleDisconnect(sessionId);
                return;
            }
        }

        // Handle player leaving room on disconnect (only if game not started)
        Room room = roomService.leaveRoom(sessionId);
        
        // Broadcast room update if room still exists
        if (room != null && roomId != null) {
            RoomMessage update = RoomMessage.builder()
                    .type("ROOM_UPDATE")
                    .roomId(room.getId())
                    .roomName(room.getName())
                    .payload(room)
                    .build();
            messagingTemplate.convertAndSend("/topic/room/" + roomId, update);
            log.info("Player disconnected from room {}, new host: {}", roomId, room.getHostId());
        }
        
        log.info("WebSocket disconnected: {}", sessionId);
    }
}

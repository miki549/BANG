package com.example.bang.controller;

import com.example.bang.dto.RoomMessage;
import com.example.bang.model.Room;
import com.example.bang.service.GameService;
import com.example.bang.service.RoomService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.util.Map;

@Slf4j
@Controller
@RequiredArgsConstructor
public class LobbyController {

    private final RoomService roomService;
    private final GameService gameService;
    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/room/create")
    public void createRoom(@Payload RoomMessage message, SimpMessageHeaderAccessor headerAccessor) {
        String sessionId = headerAccessor.getSessionId();
        String principalName = headerAccessor.getUser() != null ? headerAccessor.getUser().getName() : sessionId;
        String roomName = message.getRoomName() != null ? message.getRoomName() : "Game Room";
        String playerName = message.getPlayerName() != null ? message.getPlayerName() : "Player";

        try {
            Room room = roomService.createRoom(roomName, sessionId, playerName);
            
            RoomMessage response = RoomMessage.builder()
                    .type("ROOM_CREATED")
                    .roomId(room.getId())
                    .roomName(room.getName())
                    .playerId(room.getHostId())
                    .payload(room)
                    .build();

            messagingTemplate.convertAndSendToUser(principalName, "/queue/lobby", response);
            broadcastRoomUpdate(room);
            
            log.info("Room created: {} by {}", room.getId(), playerName);
        } catch (Exception e) {
            sendError(headerAccessor, "Failed to create room: " + e.getMessage());
        }
    }

    @MessageMapping("/room/join")
    public void joinRoom(@Payload RoomMessage message, SimpMessageHeaderAccessor headerAccessor) {
        String sessionId = headerAccessor.getSessionId();
        String principalName = headerAccessor.getUser() != null ? headerAccessor.getUser().getName() : sessionId;
        String roomId = message.getRoomId();
        String playerName = message.getPlayerName() != null ? message.getPlayerName() : "Player";

        try {
            Room room = roomService.joinRoom(roomId, sessionId, playerName);
            String playerId = roomService.getPlayerIdForSession(sessionId);

            RoomMessage response = RoomMessage.builder()
                    .type("ROOM_JOINED")
                    .roomId(room.getId())
                    .roomName(room.getName())
                    .playerId(playerId)
                    .payload(room)
                    .build();

            messagingTemplate.convertAndSendToUser(principalName, "/queue/lobby", response);
            broadcastRoomUpdate(room);
            
            log.info("Player {} joined room {}", playerName, roomId);
        } catch (Exception e) {
            sendError(headerAccessor, "Failed to join room: " + e.getMessage());
        }
    }

    @MessageMapping("/room/leave")
    public void leaveRoom(SimpMessageHeaderAccessor headerAccessor) {
        String sessionId = headerAccessor.getSessionId();
        String principalName = headerAccessor.getUser() != null ? headerAccessor.getUser().getName() : sessionId;
        String roomId = roomService.getRoomIdForSession(sessionId);

        if (roomId != null) {
            Room room = roomService.leaveRoom(sessionId);
            
            RoomMessage response = RoomMessage.builder()
                    .type("ROOM_LEFT")
                    .build();
            messagingTemplate.convertAndSendToUser(principalName, "/queue/lobby", response);

            if (room != null) {
                broadcastRoomUpdate(room);
            }
        }
    }

    @MessageMapping("/room/ready")
    public void setReady(@Payload Map<String, Boolean> payload, SimpMessageHeaderAccessor headerAccessor) {
        String sessionId = headerAccessor.getSessionId();
        boolean ready = payload.getOrDefault("ready", false);

        roomService.setPlayerReady(sessionId, ready);

        String roomId = roomService.getRoomIdForSession(sessionId);
        if (roomId != null) {
            roomService.getRoom(roomId).ifPresent(this::broadcastRoomUpdate);
        }
    }

    @MessageMapping("/room/start")
    public void startGame(SimpMessageHeaderAccessor headerAccessor) {
        String sessionId = headerAccessor.getSessionId();
        String roomId = roomService.getRoomIdForSession(sessionId);
        String playerId = roomService.getPlayerIdForSession(sessionId);

        if (roomId == null) {
            sendError(headerAccessor, "Not in a room");
            return;
        }

        Room room = roomService.getRoom(roomId).orElse(null);
        if (room == null) {
            sendError(headerAccessor, "Room not found");
            return;
        }

        if (!playerId.equals(room.getHostId())) {
            sendError(headerAccessor, "Only the host can start the game");
            return;
        }

        if (!roomService.canStartGame(roomId)) {
            sendError(headerAccessor, "Cannot start game. Need 4-7 ready players.");
            return;
        }

        roomService.markGameStarted(roomId);
        gameService.initializeGame(room);

        RoomMessage startMessage = RoomMessage.builder()
                .type("GAME_STARTED")
                .roomId(roomId)
                .build();
        messagingTemplate.convertAndSend("/topic/room/" + roomId, startMessage);

        log.info("Game started in room {}", roomId);
    }

    private void broadcastRoomUpdate(Room room) {
        RoomMessage update = RoomMessage.builder()
                .type("ROOM_UPDATE")
                .roomId(room.getId())
                .roomName(room.getName())
                .payload(room)
                .build();
        messagingTemplate.convertAndSend("/topic/room/" + room.getId(), update);
    }

    private void sendError(SimpMessageHeaderAccessor headerAccessor, String message) {
        String principalName = headerAccessor.getUser() != null ? headerAccessor.getUser().getName() : headerAccessor.getSessionId();
        RoomMessage error = RoomMessage.builder()
                .type("ERROR")
                .payload(message)
                .build();
        messagingTemplate.convertAndSendToUser(principalName, "/queue/lobby", error);
    }
}

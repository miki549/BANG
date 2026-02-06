package com.example.bang.controller;

import com.example.bang.dto.GameMessage;
import com.example.bang.dto.GameStateView;
import com.example.bang.model.GameState;
import com.example.bang.service.GameService;
import com.example.bang.service.RoomService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Slf4j
@Controller
@RequiredArgsConstructor
public class GameController {

    private final GameService gameService;
    private final RoomService roomService;
    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/game/state")
    public void getGameState(SimpMessageHeaderAccessor headerAccessor) {
        String sessionId = headerAccessor.getSessionId();
        String principalName = headerAccessor.getUser() != null ? headerAccessor.getUser().getName() : sessionId;
        String roomId = roomService.getRoomIdForSession(sessionId);
        String playerId = roomService.getPlayerIdForSession(sessionId);

        if (roomId == null || playerId == null) {
            sendError(headerAccessor, "Not in a game");
            return;
        }

        GameState state = gameService.getGame(roomId);
        if (state == null) {
            sendError(headerAccessor, "Game not found");
            return;
        }

        GameStateView view = GameStateView.fromGameState(state, playerId);
        messagingTemplate.convertAndSendToUser(principalName, "/queue/game", view);
        log.info("Sent game state to player {} in room {}", playerId, roomId);
    }

    @MessageMapping("/game/draw")
    public void drawCards(SimpMessageHeaderAccessor headerAccessor) {
        String sessionId = headerAccessor.getSessionId();
        String roomId = roomService.getRoomIdForSession(sessionId);
        String playerId = roomService.getPlayerIdForSession(sessionId);

        if (roomId == null || playerId == null) {
            sendError(headerAccessor, "Not in a game");
            return;
        }

        gameService.drawCards(roomId, playerId);
        log.debug("Player {} drew cards in room {}", playerId, roomId);
    }

    @MessageMapping("/game/play")
    public void playCard(@Payload GameMessage message, SimpMessageHeaderAccessor headerAccessor) {
        String sessionId = headerAccessor.getSessionId();
        String roomId = roomService.getRoomIdForSession(sessionId);
        String playerId = roomService.getPlayerIdForSession(sessionId);

        if (roomId == null || playerId == null) {
            sendError(headerAccessor, "Not in a game");
            return;
        }

        gameService.playCard(roomId, playerId, message.getCardId(), message.getTargetPlayerId(), message.getTargetCardId());
        log.debug("Player {} played card {} targeting {} (card: {}) in room {}",
                playerId, message.getCardId(), message.getTargetPlayerId(), message.getTargetCardId(), roomId);
    }

    @MessageMapping("/game/pass")
    public void passTurn(SimpMessageHeaderAccessor headerAccessor) {
        String sessionId = headerAccessor.getSessionId();
        String roomId = roomService.getRoomIdForSession(sessionId);
        String playerId = roomService.getPlayerIdForSession(sessionId);

        if (roomId == null || playerId == null) {
            sendError(headerAccessor, "Not in a game");
            return;
        }

        gameService.passTurn(roomId, playerId);
        log.debug("Player {} passed turn in room {}", playerId, roomId);
    }

    @MessageMapping("/game/discard")
    public void discardCard(@Payload GameMessage message, SimpMessageHeaderAccessor headerAccessor) {
        String sessionId = headerAccessor.getSessionId();
        String roomId = roomService.getRoomIdForSession(sessionId);
        String playerId = roomService.getPlayerIdForSession(sessionId);

        if (roomId == null || playerId == null) {
            sendError(headerAccessor, "Not in a game");
            return;
        }

        gameService.discardCard(roomId, playerId, message.getCardId());
        log.debug("Player {} discarded card {} in room {}", playerId, message.getCardId(), roomId);
    }

    @MessageMapping("/game/respond")
    public void respondToAction(@Payload GameMessage message, SimpMessageHeaderAccessor headerAccessor) {
        String sessionId = headerAccessor.getSessionId();
        String roomId = roomService.getRoomIdForSession(sessionId);
        String playerId = roomService.getPlayerIdForSession(sessionId);

        if (roomId == null || playerId == null) {
            sendError(headerAccessor, "Not in a game");
            return;
        }

        boolean accept = "RESPOND".equals(message.getType());
        gameService.respondToAction(roomId, playerId, message.getCardId(), accept);
        log.debug("Player {} responded to action in room {}", playerId, roomId);
    }

    @MessageMapping("/game/useAbility")
    public void useAbility(@Payload GameMessage message, SimpMessageHeaderAccessor headerAccessor) {
        String sessionId = headerAccessor.getSessionId();
        String roomId = roomService.getRoomIdForSession(sessionId);
        String playerId = roomService.getPlayerIdForSession(sessionId);

        if (roomId == null || playerId == null) {
            sendError(headerAccessor, "Not in a game");
            return;
        }

        gameService.useAbility(roomId, playerId, message.getCardId());
        log.debug("Player {} used ability {} in room {}", playerId, message.getCardId(), roomId);
    }

    private void sendError(SimpMessageHeaderAccessor headerAccessor, String errorMessage) {
        String principalName = headerAccessor.getUser() != null ? headerAccessor.getUser().getName() : headerAccessor.getSessionId();
        GameMessage error = GameMessage.builder()
                .type("ERROR")
                .payload(errorMessage)
                .build();
        messagingTemplate.convertAndSendToUser(principalName, "/queue/game", error);
    }
}

package com.example.bang.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GameEvent {
    private String type;
    private String sourcePlayerId;
    private String sourcePlayerName;
    private String targetPlayerId;
    private String targetPlayerName;
    private String cardType;
    private String cardId;
    private Object data;
    private long timestamp;

    public static GameEvent create(String type) {
        return GameEvent.builder()
                .type(type)
                .timestamp(System.currentTimeMillis())
                .build();
    }

    public static GameEvent cardPlayed(String sourcePlayerId, String sourcePlayerName, 
                                        String targetPlayerId, String targetPlayerName,
                                        String cardType, String cardId) {
        return GameEvent.builder()
                .type("CARD_PLAYED")
                .sourcePlayerId(sourcePlayerId)
                .sourcePlayerName(sourcePlayerName)
                .targetPlayerId(targetPlayerId)
                .targetPlayerName(targetPlayerName)
                .cardType(cardType)
                .cardId(cardId)
                .timestamp(System.currentTimeMillis())
                .build();
    }

    public static GameEvent playerDamaged(String playerId, String playerName, int damage, int newHealth) {
        return GameEvent.builder()
                .type("PLAYER_DAMAGED")
                .targetPlayerId(playerId)
                .targetPlayerName(playerName)
                .data(new DamageData(damage, newHealth))
                .timestamp(System.currentTimeMillis())
                .build();
    }

    public static GameEvent playerEliminated(String playerId, String playerName, String role) {
        return GameEvent.builder()
                .type("PLAYER_ELIMINATED")
                .targetPlayerId(playerId)
                .targetPlayerName(playerName)
                .data(role)
                .timestamp(System.currentTimeMillis())
                .build();
    }

    @Data
    @AllArgsConstructor
    public static class DamageData {
        private int damage;
        private int newHealth;
    }
}

package com.example.bang.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GameMessage {
    private String type;
    private String playerId;
    private String targetPlayerId;
    private String cardId;
    private String roomId;
    private Object payload;
}

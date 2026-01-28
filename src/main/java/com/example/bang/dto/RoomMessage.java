package com.example.bang.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoomMessage {
    private String type;
    private String roomId;
    private String roomName;
    private String playerId;
    private String playerName;
    private Object payload;
}

package com.example.bang.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlayerInfo {
    private String id;
    private String sessionId;
    private String principalName;
    private String name;
    private boolean ready;
    private boolean isHost;
}

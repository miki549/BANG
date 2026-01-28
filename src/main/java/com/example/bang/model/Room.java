package com.example.bang.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Room {
    private String id;
    private String name;
    private String hostId;
    
    @Builder.Default
    private List<PlayerInfo> players = new ArrayList<>();
    
    @Builder.Default
    private int minPlayers = 4;
    
    @Builder.Default
    private int maxPlayers = 7;
    
    private boolean gameStarted;

    public boolean canStart() {
        return players.size() >= minPlayers && players.size() <= maxPlayers;
    }

    public boolean isFull() {
        return players.size() >= maxPlayers;
    }

    public void addPlayer(PlayerInfo player) {
        if (!isFull() && !gameStarted) {
            players.add(player);
        }
    }

    public void removePlayer(String playerId) {
        players.removeIf(p -> p.getId().equals(playerId));
    }

    public PlayerInfo getPlayer(String playerId) {
        return players.stream()
                .filter(p -> p.getId().equals(playerId))
                .findFirst()
                .orElse(null);
    }
}

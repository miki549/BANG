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
public class GameState {
    private String roomId;
    private String roomName;
    private String hostPlayerId;
    
    @Builder.Default
    private List<Player> players = new ArrayList<>();
    
    @Builder.Default
    private List<Card> drawPile = new ArrayList<>();
    
    @Builder.Default
    private List<Card> discardPile = new ArrayList<>();
    
    private int currentPlayerIndex;
    private GamePhase phase;
    
    private String pendingActionPlayerId;
    @Builder.Default
    private List<String> pendingActionPlayers = new ArrayList<>();
    private String pendingActionType;
    private String pendingActionSourcePlayerId;
    private Card pendingActionCard;
    private int missedCardsRequired;

    @Builder.Default
    private java.util.Set<String> usedReactionAbilities = new java.util.HashSet<>();
    
    @Builder.Default
    private List<String> generalStoreCards = new ArrayList<>();
    
    private String winnerId;
    private Role winningTeam;

    public Player getCurrentPlayer() {
        if (players.isEmpty() || currentPlayerIndex < 0 || currentPlayerIndex >= players.size()) {
            return null;
        }
        return players.get(currentPlayerIndex);
    }

    public Player getPlayerById(String playerId) {
        return players.stream()
                .filter(p -> p.getId().equals(playerId))
                .findFirst()
                .orElse(null);
    }

    public Player getPlayerBySessionId(String sessionId) {
        return players.stream()
                .filter(p -> p.getSessionId().equals(sessionId))
                .findFirst()
                .orElse(null);
    }

    public List<Player> getAlivePlayers() {
        return players.stream()
                .filter(Player::isAlive)
                .toList();
    }

    public int getAlivePlayerCount() {
        return (int) players.stream().filter(Player::isAlive).count();
    }

    public void nextPlayer() {
        if (players.isEmpty()) return;
        
        int startIndex = currentPlayerIndex;
        do {
            currentPlayerIndex = (currentPlayerIndex + 1) % players.size();
            if (players.get(currentPlayerIndex).isAlive()) {
                return;
            }
        } while (currentPlayerIndex != startIndex);
    }

    public int calculateDistance(Player from, Player to) {
        List<Player> alivePlayers = getAlivePlayers();
        int fromPos = alivePlayers.indexOf(from);
        int toPos = alivePlayers.indexOf(to);
        
        if (fromPos == -1 || toPos == -1) return Integer.MAX_VALUE;
        
        int size = alivePlayers.size();
        int clockwise = Math.abs(toPos - fromPos);
        int counterClockwise = size - clockwise;
        
        int baseDistance = Math.min(clockwise, counterClockwise);
        
        int modifiedDistance = baseDistance + to.getDistanceModifierIncoming() + from.getDistanceModifierOutgoing();
        
        return Math.max(1, modifiedDistance);
    }

    public boolean canTarget(Player attacker, Player target) {
        if (!target.isAlive() || attacker.getId().equals(target.getId())) {
            return false;
        }
        int distance = calculateDistance(attacker, target);
        return distance <= attacker.getWeaponRange();
    }

    public Card drawCard() {
        if (drawPile.isEmpty()) {
            reshuffleDiscardPile();
        }
        if (drawPile.isEmpty()) {
            return null;
        }
        return drawPile.remove(0);
    }

    public void discardCard(Card card) {
        if (card != null) {
            discardPile.add(card);
        }
    }

    private void reshuffleDiscardPile() {
        if (discardPile.isEmpty()) return;
        
        Card topCard = discardPile.remove(discardPile.size() - 1);
        drawPile.addAll(discardPile);
        discardPile.clear();
        discardPile.add(topCard);
        
        java.util.Collections.shuffle(drawPile);
    }

    public boolean isGameOver() {
        return phase == GamePhase.GAME_OVER;
    }

    public Player getSheriff() {
        return players.stream()
                .filter(p -> p.getRole() == Role.SHERIFF)
                .findFirst()
                .orElse(null);
    }

    public boolean isSheriffAlive() {
        Player sheriff = getSheriff();
        return sheriff != null && sheriff.isAlive();
    }
}

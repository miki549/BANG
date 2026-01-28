package com.example.bang.dto;

import com.example.bang.model.*;
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
public class GameStateView {
    private String roomId;
    private GamePhase phase;
    private String currentPlayerId;
    private String currentPlayerName;
    private List<PlayerView> players;
    private int drawPileSize;
    private int discardPileSize;
    private Card topDiscardCard;
    private String pendingActionPlayerId;
    private String pendingActionType;
    private String pendingActionSourcePlayerId;
    private int missedCardsRequired;
    private List<Card> generalStoreCards;
    private String winnerId;
    private Role winningTeam;
    private List<GameEvent> recentEvents;

    public static GameStateView fromGameState(GameState state, String requestingPlayerId) {
        List<PlayerView> playerViews = new ArrayList<>();
        
        for (Player player : state.getPlayers()) {
            boolean includeHand = player.getId().equals(requestingPlayerId);
            boolean includeRole = player.isSheriff() || 
                                  player.getId().equals(requestingPlayerId) ||
                                  !player.isAlive() ||
                                  state.getPhase() == GamePhase.GAME_OVER;
            playerViews.add(PlayerView.fromPlayer(player, includeHand, includeRole));
        }

        Player currentPlayer = state.getCurrentPlayer();
        Card topDiscard = state.getDiscardPile().isEmpty() ? null : 
                          state.getDiscardPile().get(state.getDiscardPile().size() - 1);

        return GameStateView.builder()
                .roomId(state.getRoomId())
                .phase(state.getPhase())
                .currentPlayerId(currentPlayer != null ? currentPlayer.getId() : null)
                .currentPlayerName(currentPlayer != null ? currentPlayer.getName() : null)
                .players(playerViews)
                .drawPileSize(state.getDrawPile().size())
                .discardPileSize(state.getDiscardPile().size())
                .topDiscardCard(topDiscard)
                .pendingActionPlayerId(state.getPendingActionPlayerId())
                .pendingActionType(state.getPendingActionType())
                .pendingActionSourcePlayerId(state.getPendingActionSourcePlayerId())
                .missedCardsRequired(state.getMissedCardsRequired())
                .generalStoreCards(null)
                .winnerId(state.getWinnerId())
                .winningTeam(state.getWinningTeam())
                .build();
    }
}

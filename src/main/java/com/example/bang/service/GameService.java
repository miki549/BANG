package com.example.bang.service;

import com.example.bang.dto.GameEvent;
import com.example.bang.dto.GameStateView;
import com.example.bang.model.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class GameService {

    private final DeckBuilder deckBuilder;
    private final SimpMessagingTemplate messagingTemplate;
    private final Map<String, GameState> games = new ConcurrentHashMap<>();

    public GameState initializeGame(Room room) {
        List<PlayerInfo> roomPlayers = room.getPlayers();
        int playerCount = roomPlayers.size();

        List<Role> roles = deckBuilder.getRolesForPlayerCount(playerCount);
        List<CharacterType> characters = deckBuilder.getRandomCharacters(playerCount);
        List<Card> deck = deckBuilder.createDeck();

        List<Player> players = new ArrayList<>();
        int sheriffIndex = 0;

        for (int i = 0; i < playerCount; i++) {
            PlayerInfo info = roomPlayers.get(i);
            Role role = roles.get(i);
            CharacterType character = characters.get(i);

            int maxHealth = character.getMaxHealth();
            if (role == Role.SHERIFF) {
                maxHealth++;
                sheriffIndex = i;
            }

            Player player = Player.builder()
                    .id(info.getId())
                    .sessionId(info.getSessionId())
                    .principalName(info.getPrincipalName())
                    .name(info.getName())
                    .role(role)
                    .character(character)
                    .health(maxHealth)
                    .maxHealth(maxHealth)
                    .alive(true)
                    .isSheriff(role == Role.SHERIFF)
                    .hand(new ArrayList<>())
                    .inPlay(new ArrayList<>())
                    .seatPosition(i)
                    .build();

            // Deal initial cards equal to health
            for (int j = 0; j < maxHealth && !deck.isEmpty(); j++) {
                player.addCardToHand(deck.remove(0));
            }

            players.add(player);
        }

        GameState gameState = GameState.builder()
                .roomId(room.getId())
                .roomName(room.getName())
                .hostPlayerId(room.getHostId())
                .players(players)
                .drawPile(deck)
                .discardPile(new ArrayList<>())
                .currentPlayerIndex(sheriffIndex)
                .phase(GamePhase.DRAW_PHASE)
                .build();

        games.put(room.getId(), gameState);
        
        // Broadcast initial game state to all players
        broadcastGameState(room.getId());
        log.info("Game initialized for room {} with {} players", room.getId(), playerCount);
        
        return gameState;
    }

    public GameState getGame(String roomId) {
        return games.get(roomId);
    }

    public void updatePlayerSession(String roomId, String playerId, String newSessionId, String newPrincipalName) {
        GameState state = games.get(roomId);
        if (state != null) {
            Player player = state.getPlayerById(playerId);
            if (player != null) {
                player.setSessionId(newSessionId);
                player.setPrincipalName(newPrincipalName);
            }
        }
    }

    public void drawCards(String roomId, String playerId) {
        GameState state = games.get(roomId);
        if (state == null) return;

        Player player = state.getPlayerById(playerId);
        if (player == null || !player.getId().equals(state.getCurrentPlayer().getId())) {
            return;
        }

        if (state.getPhase() != GamePhase.DRAW_PHASE) {
            return;
        }

        // Handle character abilities for drawing
        int cardsToDraw = 2;
        
        // Kit Carlson looks at 3 cards
        if (player.getCharacter() == CharacterType.KIT_CARLSON) {
            // Special handling needed - for now just draw 2
            for (int i = 0; i < cardsToDraw; i++) {
                Card card = state.drawCard();
                if (card != null) {
                    player.addCardToHand(card);
                }
            }
        } else if (player.getCharacter() == CharacterType.BLACK_JACK) {
            // Draw first card
            Card first = state.drawCard();
            if (first != null) player.addCardToHand(first);
            
            // Draw second and check for bonus
            Card second = state.drawCard();
            if (second != null) {
                player.addCardToHand(second);
                if (second.getSuit() == CardSuit.HEARTS || second.getSuit() == CardSuit.DIAMONDS) {
                    Card bonus = state.drawCard();
                    if (bonus != null) player.addCardToHand(bonus);
                }
            }
        } else {
            for (int i = 0; i < cardsToDraw; i++) {
                Card card = state.drawCard();
                if (card != null) {
                    player.addCardToHand(card);
                }
            }
        }

        state.setPhase(GamePhase.PLAY_PHASE);
        broadcastGameState(roomId);
    }

    public void playCard(String roomId, String playerId, String cardId, String targetPlayerId) {
        GameState state = games.get(roomId);
        if (state == null) return;

        Player player = state.getPlayerById(playerId);
        if (player == null) return;

        // Check if it's this player's turn (unless reacting)
        if (state.getPhase() != GamePhase.REACTION_PHASE && 
            !player.getId().equals(state.getCurrentPlayer().getId())) {
            return;
        }

        Card card = player.findCardInHand(cardId);
        if (card == null) return;

        Player target = targetPlayerId != null ? state.getPlayerById(targetPlayerId) : null;

        boolean played = processCardPlay(state, player, card, target);
        
        if (played) {
            player.removeCardFromHand(card);
            
            if (card.isBrownCard()) {
                state.discardCard(card);
            } else {
                // Blue cards go into play
                if (card.isWeapon()) {
                    if (player.getWeapon() != null) {
                        state.discardCard(player.getWeapon());
                    }
                    player.setWeapon(card);
                } else {
                    player.getInPlay().add(card);
                }
            }

            // Broadcast event
            GameEvent event = GameEvent.cardPlayed(
                    player.getId(), player.getName(),
                    target != null ? target.getId() : null,
                    target != null ? target.getName() : null,
                    card.getType().name(), card.getId()
            );
            broadcastEvent(roomId, event);
        }

        checkGameEnd(state);
        broadcastGameState(roomId);
    }

    private boolean processCardPlay(GameState state, Player player, Card card, Player target) {
        switch (card.getType()) {
            case BANG:
                return processBang(state, player, card, target);
            case MISSED:
                return processMissed(state, player, card);
            case BEER:
                return processBeer(state, player);
            case PANIC:
                return processPanic(state, player, target);
            case CAT_BALOU:
                return processCatBalou(state, player, target);
            case STAGECOACH:
                return processStagecoach(state, player);
            case WELLS_FARGO:
                return processWellsFargo(state, player);
            case DUEL:
                return processDuel(state, player, target);
            case GATLING:
                return processGatling(state, player);
            case INDIANS:
                return processIndians(state, player);
            case SALOON:
                return processSaloon(state);
            case GENERAL_STORE:
                return processGeneralStore(state, player);
            case BARREL:
            case MUSTANG:
            case SCOPE:
            case VOLCANIC:
            case SCHOFIELD:
            case REMINGTON:
            case REV_CARABINE:
            case WINCHESTER:
                return true; // Blue cards just need to be played
            case JAIL:
                return processJail(state, player, target);
            case DYNAMITE:
                return true; // Goes into play
            default:
                return false;
        }
    }

    private boolean processBang(GameState state, Player player, Card card, Player target) {
        if (target == null || !target.isAlive()) return false;
        if (!state.canTarget(player, target)) return false;
        if (!player.canPlayBang()) return false;

        player.setBangsPlayedThisTurn(player.getBangsPlayedThisTurn() + 1);

        // Set up reaction phase
        state.setPhase(GamePhase.REACTION_PHASE);
        state.setPendingActionPlayerId(target.getId());
        state.setPendingActionSourcePlayerId(player.getId());
        state.setPendingActionType("BANG");
        state.setPendingActionCard(card);
        
        int missedRequired = 1;
        if (player.getCharacter() == CharacterType.SLAB_THE_KILLER) {
            missedRequired = 2;
        }
        state.setMissedCardsRequired(missedRequired);

        return true;
    }

    private boolean processMissed(GameState state, Player player, Card card) {
        if (state.getPhase() != GamePhase.REACTION_PHASE) return false;
        if (!player.getId().equals(state.getPendingActionPlayerId())) return false;
        if (!"BANG".equals(state.getPendingActionType()) && !"GATLING".equals(state.getPendingActionType()) && !"INDIANS".equals(state.getPendingActionType())) {
            return false;
        }

        int remaining = state.getMissedCardsRequired() - 1;
        state.setMissedCardsRequired(remaining);

        if (remaining <= 0) {
            // Successfully dodged
            clearPendingAction(state);
        }

        return true;
    }

    private boolean processBeer(GameState state, Player player) {
        // Beer has no effect with only 2 players
        if (state.getAlivePlayerCount() <= 2) return false;
        
        if (player.getHealth() < player.getMaxHealth()) {
            player.heal(1);
            
            // Trigger Suzy Lafayette ability
            if (player.getCharacter() == CharacterType.SUZY_LAFAYETTE && player.getHand().isEmpty()) {
                Card drawn = state.drawCard();
                if (drawn != null) player.addCardToHand(drawn);
            }
            
            return true;
        }
        return false;
    }

    private boolean processPanic(GameState state, Player player, Player target) {
        if (target == null || !target.isAlive()) return false;
        if (state.calculateDistance(player, target) > 1) return false;
        
        // Steal a random card from target's hand
        if (!target.getHand().isEmpty()) {
            int index = new Random().nextInt(target.getHand().size());
            Card stolen = target.getHand().remove(index);
            player.addCardToHand(stolen);
        } else if (!target.getInPlay().isEmpty()) {
            // Or take a card in play
            Card stolen = target.getInPlay().remove(0);
            player.addCardToHand(stolen);
        } else {
            return false;
        }
        return true;
    }

    private boolean processCatBalou(GameState state, Player player, Player target) {
        if (target == null || !target.isAlive()) return false;
        
        // Discard a random card from target's hand or a card in play
        if (!target.getHand().isEmpty()) {
            int index = new Random().nextInt(target.getHand().size());
            Card discarded = target.getHand().remove(index);
            state.discardCard(discarded);
        } else if (!target.getInPlay().isEmpty()) {
            Card discarded = target.getInPlay().remove(0);
            state.discardCard(discarded);
        } else {
            return false;
        }
        return true;
    }

    private boolean processStagecoach(GameState state, Player player) {
        for (int i = 0; i < 2; i++) {
            Card card = state.drawCard();
            if (card != null) player.addCardToHand(card);
        }
        return true;
    }

    private boolean processWellsFargo(GameState state, Player player) {
        for (int i = 0; i < 3; i++) {
            Card card = state.drawCard();
            if (card != null) player.addCardToHand(card);
        }
        return true;
    }

    private boolean processDuel(GameState state, Player player, Player target) {
        if (target == null || !target.isAlive()) return false;
        
        state.setPhase(GamePhase.REACTION_PHASE);
        state.setPendingActionPlayerId(target.getId());
        state.setPendingActionSourcePlayerId(player.getId());
        state.setPendingActionType("DUEL");
        state.setMissedCardsRequired(1);
        
        return true;
    }

    private boolean processGatling(GameState state, Player player) {
        // Hits all other players
        for (Player target : state.getAlivePlayers()) {
            if (!target.getId().equals(player.getId())) {
                state.setPhase(GamePhase.REACTION_PHASE);
                state.setPendingActionPlayerId(target.getId());
                state.setPendingActionSourcePlayerId(player.getId());
                state.setPendingActionType("GATLING");
                state.setMissedCardsRequired(1);
                // For simplicity, process one at a time
                break;
            }
        }
        return true;
    }

    private boolean processIndians(GameState state, Player player) {
        // All other players must play BANG or lose 1 HP
        for (Player target : state.getAlivePlayers()) {
            if (!target.getId().equals(player.getId())) {
                state.setPhase(GamePhase.REACTION_PHASE);
                state.setPendingActionPlayerId(target.getId());
                state.setPendingActionSourcePlayerId(player.getId());
                state.setPendingActionType("INDIANS");
                state.setMissedCardsRequired(1);
                break;
            }
        }
        return true;
    }

    private boolean processSaloon(GameState state) {
        for (Player p : state.getAlivePlayers()) {
            if (p.getHealth() < p.getMaxHealth()) {
                p.heal(1);
            }
        }
        return true;
    }

    private boolean processGeneralStore(GameState state, Player player) {
        // Draw cards equal to number of alive players
        List<String> storeCards = new ArrayList<>();
        for (int i = 0; i < state.getAlivePlayerCount(); i++) {
            Card card = state.drawCard();
            if (card != null) {
                storeCards.add(card.getId());
            }
        }
        state.setGeneralStoreCards(storeCards);
        return true;
    }

    private boolean processJail(GameState state, Player player, Player target) {
        if (target == null || !target.isAlive()) return false;
        if (target.isSheriff()) return false; // Can't jail the Sheriff
        return true;
    }

    public void passTurn(String roomId, String playerId) {
        GameState state = games.get(roomId);
        if (state == null) return;

        Player player = state.getPlayerById(playerId);
        if (player == null || !player.getId().equals(state.getCurrentPlayer().getId())) {
            return;
        }

        if (state.getPhase() != GamePhase.PLAY_PHASE) return;

        // Check if player needs to discard
        if (player.getHand().size() > player.getHandLimit()) {
            state.setPhase(GamePhase.DISCARD_PHASE);
            broadcastGameState(roomId);
            return;
        }

        endTurn(state);
        broadcastGameState(roomId);
    }

    public void discardCard(String roomId, String playerId, String cardId) {
        GameState state = games.get(roomId);
        if (state == null) return;

        Player player = state.getPlayerById(playerId);
        if (player == null || !player.getId().equals(state.getCurrentPlayer().getId())) {
            return;
        }

        if (state.getPhase() != GamePhase.DISCARD_PHASE) return;

        Card card = player.findCardInHand(cardId);
        if (card == null) return;

        player.removeCardFromHand(card);
        state.discardCard(card);

        if (player.getHand().size() <= player.getHandLimit()) {
            endTurn(state);
        }

        broadcastGameState(roomId);
    }

    public void respondToAction(String roomId, String playerId, String cardId, boolean accept) {
        GameState state = games.get(roomId);
        if (state == null) return;
        if (state.getPhase() != GamePhase.REACTION_PHASE) return;

        Player player = state.getPlayerById(playerId);
        if (player == null || !player.getId().equals(state.getPendingActionPlayerId())) {
            return;
        }

        String actionType = state.getPendingActionType();

        if (accept && cardId != null) {
            Card card = player.findCardInHand(cardId);
            if (card != null) {
                boolean validResponse = false;
                
                if ("BANG".equals(actionType) || "GATLING".equals(actionType)) {
                    // Need to play Missed
                    if (card.getType() == CardType.MISSED || 
                        (player.getCharacter() == CharacterType.CALAMITY_JANET && card.getType() == CardType.BANG)) {
                        validResponse = true;
                    }
                } else if ("INDIANS".equals(actionType) || "DUEL".equals(actionType)) {
                    // Need to play Bang
                    if (card.getType() == CardType.BANG ||
                        (player.getCharacter() == CharacterType.CALAMITY_JANET && card.getType() == CardType.MISSED)) {
                        validResponse = true;
                    }
                }

                if (validResponse) {
                    player.removeCardFromHand(card);
                    state.discardCard(card);
                    
                    int remaining = state.getMissedCardsRequired() - 1;
                    state.setMissedCardsRequired(remaining);

                    if (remaining <= 0) {
                        if ("DUEL".equals(actionType)) {
                            // Duel continues - swap attacker and defender
                            String sourceId = state.getPendingActionSourcePlayerId();
                            state.setPendingActionPlayerId(sourceId);
                            state.setPendingActionSourcePlayerId(playerId);
                            state.setMissedCardsRequired(1);
                        } else {
                            clearPendingAction(state);
                        }
                    }
                }
            }
        } else {
            // Player takes damage
            applyDamage(state, player, 1, state.getPlayerById(state.getPendingActionSourcePlayerId()));
            clearPendingAction(state);
        }

        checkGameEnd(state);
        broadcastGameState(roomId);
    }

    private void applyDamage(GameState state, Player target, int amount, Player source) {
        target.takeDamage(amount);

        // Bart Cassidy draws a card when hit
        if (target.getCharacter() == CharacterType.BART_CASSIDY && target.isAlive()) {
            Card drawn = state.drawCard();
            if (drawn != null) target.addCardToHand(drawn);
        }

        // El Gringo steals from attacker
        if (target.getCharacter() == CharacterType.EL_GRINGO && source != null && !source.getHand().isEmpty()) {
            int index = new Random().nextInt(source.getHand().size());
            Card stolen = source.getHand().remove(index);
            target.addCardToHand(stolen);
        }

        if (!target.isAlive()) {
            handlePlayerDeath(state, target, source);
        }

        broadcastEvent(state.getRoomId(), GameEvent.playerDamaged(
                target.getId(), target.getName(), amount, target.getHealth()
        ));
    }

    private void handlePlayerDeath(GameState state, Player eliminated, Player killer) {
        // Vulture Sam takes all cards
        for (Player p : state.getAlivePlayers()) {
            if (p.getCharacter() == CharacterType.VULTURE_SAM) {
                p.getHand().addAll(eliminated.getHand());
                p.getHand().addAll(eliminated.getInPlay());
                if (eliminated.getWeapon() != null) {
                    p.getHand().add(eliminated.getWeapon());
                }
                eliminated.getHand().clear();
                eliminated.getInPlay().clear();
                eliminated.setWeapon(null);
                break;
            }
        }

        // Discard remaining cards
        state.getDiscardPile().addAll(eliminated.getHand());
        state.getDiscardPile().addAll(eliminated.getInPlay());
        if (eliminated.getWeapon() != null) {
            state.discardCard(eliminated.getWeapon());
        }
        eliminated.getHand().clear();
        eliminated.getInPlay().clear();
        eliminated.setWeapon(null);

        // Rewards and penalties
        if (killer != null) {
            if (eliminated.getRole() == Role.OUTLAW) {
                // Reward: draw 3 cards
                for (int i = 0; i < 3; i++) {
                    Card card = state.drawCard();
                    if (card != null) killer.addCardToHand(card);
                }
            } else if (eliminated.getRole() == Role.DEPUTY && killer.isSheriff()) {
                // Sheriff kills deputy: discard all cards
                state.getDiscardPile().addAll(killer.getHand());
                state.getDiscardPile().addAll(killer.getInPlay());
                if (killer.getWeapon() != null) {
                    state.discardCard(killer.getWeapon());
                }
                killer.getHand().clear();
                killer.getInPlay().clear();
                killer.setWeapon(null);
            }
        }

        broadcastEvent(state.getRoomId(), GameEvent.playerEliminated(
                eliminated.getId(), eliminated.getName(), eliminated.getRole().name()
        ));
    }

    private void clearPendingAction(GameState state) {
        state.setPendingActionPlayerId(null);
        state.setPendingActionSourcePlayerId(null);
        state.setPendingActionType(null);
        state.setPendingActionCard(null);
        state.setMissedCardsRequired(0);
        state.setPhase(GamePhase.PLAY_PHASE);
    }

    private void endTurn(GameState state) {
        Player current = state.getCurrentPlayer();
        if (current != null) {
            current.resetTurn();
        }

        state.nextPlayer();
        state.setPhase(GamePhase.DRAW_PHASE);

        // Handle dynamite and jail for new current player
        Player newCurrent = state.getCurrentPlayer();
        if (newCurrent != null) {
            processTurnStart(state, newCurrent);
        }
    }

    private void processTurnStart(GameState state, Player player) {
        // Check for Dynamite
        Card dynamite = player.getInPlay().stream()
                .filter(c -> c.getType() == CardType.DYNAMITE)
                .findFirst().orElse(null);

        if (dynamite != null) {
            // Draw for dynamite check
            Card drawn = state.drawCard();
            if (drawn != null) {
                state.discardCard(drawn);
                if (drawn.getSuit() == CardSuit.SPADES && 
                    "2345678910".contains(drawn.getValue().replace("10", "10"))) {
                    // Dynamite explodes - 3 damage
                    player.getInPlay().remove(dynamite);
                    state.discardCard(dynamite);
                    applyDamage(state, player, 3, null);
                } else {
                    // Pass dynamite to next player
                    player.getInPlay().remove(dynamite);
                    Player next = getNextAlivePlayer(state, player);
                    if (next != null) {
                        next.getInPlay().add(dynamite);
                    }
                }
            }
        }

        // Check for Jail
        Card jail = player.getInPlay().stream()
                .filter(c -> c.getType() == CardType.JAIL)
                .findFirst().orElse(null);

        if (jail != null) {
            player.getInPlay().remove(jail);
            state.discardCard(jail);
            
            Card drawn = state.drawCard();
            if (drawn != null) {
                state.discardCard(drawn);
                if (drawn.getSuit() != CardSuit.HEARTS) {
                    // Stays in jail - skip turn
                    endTurn(state);
                }
            }
        }
    }

    private Player getNextAlivePlayer(GameState state, Player current) {
        List<Player> alive = state.getAlivePlayers();
        int currentIndex = alive.indexOf(current);
        if (currentIndex == -1 || alive.size() <= 1) return null;
        return alive.get((currentIndex + 1) % alive.size());
    }

    private void checkGameEnd(GameState state) {
        if (!state.isSheriffAlive()) {
            // Sheriff is dead
            List<Player> alive = state.getAlivePlayers();
            if (alive.size() == 1 && alive.get(0).getRole() == Role.RENEGADE) {
                state.setWinningTeam(Role.RENEGADE);
                state.setWinnerId(alive.get(0).getId());
            } else {
                state.setWinningTeam(Role.OUTLAW);
            }
            state.setPhase(GamePhase.GAME_OVER);
            return;
        }

        // Check if all outlaws and renegade are dead
        boolean outstandingThreats = state.getAlivePlayers().stream()
                .anyMatch(p -> p.getRole() == Role.OUTLAW || p.getRole() == Role.RENEGADE);

        if (!outstandingThreats) {
            state.setWinningTeam(Role.SHERIFF);
            state.setPhase(GamePhase.GAME_OVER);
        }
    }

    private void broadcastGameState(String roomId) {
        GameState state = games.get(roomId);
        if (state == null) return;

        // Broadcast to room topic for all players
        messagingTemplate.convertAndSend(
                "/topic/room/" + roomId + "/state",
                GameStateView.fromGameState(state, null)
        );

        // Also send personalized view to each player
        for (Player player : state.getPlayers()) {
            GameStateView view = GameStateView.fromGameState(state, player.getId());
            
            if (player.getPrincipalName() != null) {
                messagingTemplate.convertAndSendToUser(
                        player.getPrincipalName(),
                        "/queue/game",
                        view
                );
            } else {
                messagingTemplate.convertAndSend(
                        "/topic/room/" + roomId + "/player/" + player.getId(),
                        view
                );
            }
        }
    }

    private void broadcastEvent(String roomId, GameEvent event) {
        messagingTemplate.convertAndSend("/topic/room/" + roomId + "/events", event);
    }
}

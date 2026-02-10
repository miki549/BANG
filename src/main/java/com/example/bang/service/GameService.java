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

    public void selectKitCarlsonCards(String roomId, String playerId, List<String> keptCardIds) {
        GameState state = games.get(roomId);
        if (state == null) return;

        Player player = state.getPlayerById(playerId);
        if (player == null || !player.getId().equals(state.getCurrentPlayer().getId())) {
            return;
        }

        if (state.getPhase() != GamePhase.KIT_CARLSON_PHASE) {
            return;
        }

        if (player.getCharacter() != CharacterType.KIT_CARLSON) {
            return;
        }

        List<Card> drawnCards = state.getDrawnCardsToChooseFrom();
        if (drawnCards == null || drawnCards.isEmpty()) {
            return;
        }

        if (keptCardIds == null || keptCardIds.size() != 2) {
            return;
        }

        List<Card> keptCards = new ArrayList<>();
        Card returnedCard = null;

        for (Card card : drawnCards) {
            if (keptCardIds.contains(card.getId())) {
                keptCards.add(card);
            } else {
                returnedCard = card;
            }
        }

        if (keptCards.size() != 2) {
            // Invalid selection (e.g. bad IDs)
            return;
        }

        // Add kept cards to hand
        for (Card card : keptCards) {
            player.addCardToHand(card);
            // We can broadcast a generic "drawn" event so others know they got cards,
            // but without revealing which ones (though standard draw reveals nothing anyway)
        }
        // Broadcast that player drew 2 cards (effectively)
        broadcastEvent(roomId, GameEvent.cardDrawn(player.getId(), player.getName()));
        broadcastEvent(roomId, GameEvent.cardDrawn(player.getId(), player.getName()));

        // Return the other card to top of deck
        if (returnedCard != null) {
            state.getDrawPile().add(0, returnedCard);
        }

        state.setDrawnCardsToChooseFrom(new ArrayList<>());
        state.setPhase(GamePhase.PLAY_PHASE);
        broadcastGameState(roomId);
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
            List<Card> drawnCards = new ArrayList<>();
            for (int i = 0; i < 3; i++) {
                Card card = state.drawCard();
                if (card != null) {
                    drawnCards.add(card);
                }
            }
            state.setDrawnCardsToChooseFrom(drawnCards);
            state.setPhase(GamePhase.KIT_CARLSON_PHASE);
            broadcastGameState(roomId);
            return;
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
                    broadcastEvent(roomId, GameEvent.cardDrawn(player.getId(), player.getName()));
                }
            }
        }

        state.setPhase(GamePhase.PLAY_PHASE);
        broadcastGameState(roomId);
    }

    public void playCard(String roomId, String playerId, String cardId, String targetPlayerId, String targetCardId) {
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

        List<GameEvent> sideEffects = processCardPlay(state, player, card, target, targetCardId);
        
        if (sideEffects != null) {
            player.removeCardFromHand(card);
            
            if (card.isBrownCard()) {
                state.discardCard(card);
            } else {
                // Blue cards go into play
                if (card.isWeapon()) {
                    if (player.getWeapon() != null) {
                        state.discardCard(player.getWeapon());
                        broadcastEvent(roomId, GameEvent.cardDiscarded(player.getId(), player.getName(), player.getWeapon().getType().name(), player.getWeapon().getId()));
                    }
                    player.setWeapon(card);
                } else if (card.getType() == CardType.JAIL) {
                    if (target != null) {
                        target.getInPlay().add(card);
                    }
                } else {
                    // Check for existing copy of same type
                    Card existing = player.getInPlay().stream()
                            .filter(c -> c.getType() == card.getType())
                            .findFirst()
                            .orElse(null);

                    if (existing != null) {
                        player.getInPlay().remove(existing);
                        state.discardCard(existing);
                        broadcastEvent(roomId, GameEvent.cardDiscarded(player.getId(), player.getName(), existing.getType().name(), existing.getId()));
                    }
                    player.getInPlay().add(card);
                }
            }

            // 1. Broadcast played event (FIRST)
            GameEvent event = GameEvent.cardPlayed(
                    player.getId(), player.getName(),
                    target != null ? target.getId() : null,
                    target != null ? target.getName() : null,
                    card.getType().name(), card.getId()
            );
            broadcastEvent(roomId, event);

            // 2. Broadcast side effect events (SECOND)
            for (GameEvent effect : sideEffects) {
                broadcastEvent(roomId, effect);
            }
        }

        checkGameEnd(state);
        broadcastGameState(roomId);
    }

    private List<GameEvent> processCardPlay(GameState state, Player player, Card card, Player target, String targetCardId) {
        switch (card.getType()) {
            case BANG:
                return processBang(state, player, card, target);
            case MISSED:
                return processMissed(state, player, card);
            case BEER:
                return processBeer(state, player);
            case PANIC:
                return processPanic(state, player, target, targetCardId);
            case CAT_BALOU:
                return processCatBalou(state, player, target, targetCardId);
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
                return new ArrayList<>(); // Blue cards just need to be played
            case JAIL:
                return processJail(state, player, target);
            case DYNAMITE:
                return new ArrayList<>(); // Goes into play
            default:
                return null;
        }
    }

    private List<GameEvent> processBang(GameState state, Player player, Card card, Player target) {
        if (target == null || !target.isAlive()) return null;
        if (!state.canTarget(player, target)) return null;
        if (!player.canPlayBang()) return null;

        player.setBangsPlayedThisTurn(player.getBangsPlayedThisTurn() + 1);

        // Set up reaction phase
        state.setPhase(GamePhase.REACTION_PHASE);
        state.getPendingActionPlayers().clear();
        state.getPendingActionPlayers().add(target.getId());
        state.setPendingActionPlayerId(target.getId());
        state.setPendingActionSourcePlayerId(player.getId());
        state.setPendingActionType("BANG");
        state.setPendingActionCard(card);
        
        int missedRequired = 1;
        if (player.getCharacter() == CharacterType.SLAB_THE_KILLER) {
            missedRequired = 2;
        }
        state.setMissedCardsRequired(missedRequired);

        return new ArrayList<>();
    }

    private List<GameEvent> processMissed(GameState state, Player player, Card card) {
        if (state.getPhase() != GamePhase.REACTION_PHASE) return null;
        if (!player.getId().equals(state.getPendingActionPlayerId())) return null;
        if (!"BANG".equals(state.getPendingActionType()) && !"GATLING".equals(state.getPendingActionType()) && !"INDIANS".equals(state.getPendingActionType())) {
            return null;
        }

        int remaining = state.getMissedCardsRequired() - 1;
        state.setMissedCardsRequired(remaining);

        if (remaining <= 0) {
            // Successfully dodged
            clearPendingAction(state);
        }

        return new ArrayList<>();
    }

    private List<GameEvent> processBeer(GameState state, Player player) {
        // Beer has no effect with only 2 players
        if (state.getAlivePlayerCount() <= 2) return null;
        
        if (player.getHealth() < player.getMaxHealth()) {
            player.heal(1);
            
            List<GameEvent> events = new ArrayList<>();
            // Trigger Suzy Lafayette ability
            if (player.getCharacter() == CharacterType.SUZY_LAFAYETTE && player.getHand().isEmpty()) {
                Card drawn = state.drawCard();
                if (drawn != null) {
                    player.addCardToHand(drawn);
                    events.add(GameEvent.cardDrawn(player.getId(), player.getName()));
                }
            }
            
            return events;
        }
        return null;
    }

    private List<GameEvent> processPanic(GameState state, Player player, Player target, String targetCardId) {
        if (target == null || !target.isAlive()) return null;
        if (state.calculateDistance(player, target) > 1) return null;
        
        List<GameEvent> events = new ArrayList<>();
        
        Card stolen = null;

        // Try to find specific card in play if ID provided
        if (targetCardId != null) {
            stolen = target.getInPlay().stream()
                    .filter(c -> c.getId().equals(targetCardId))
                    .findFirst()
                    .orElse(null);
            
            if (stolen != null) {
                target.getInPlay().remove(stolen);
            } else if (target.getWeapon() != null && target.getWeapon().getId().equals(targetCardId)) {
                stolen = target.getWeapon();
                target.setWeapon(null);
            }
        }

        // If no specific card targeted/found, try hand or fallback logic
        if (stolen == null) {
             if (!target.getHand().isEmpty()) {
                 int index = new Random().nextInt(target.getHand().size());
                 stolen = target.getHand().remove(index);
             } else if (!target.getInPlay().isEmpty()) {
                 stolen = target.getInPlay().remove(0);
             }
        }

        if (stolen != null) {
            player.addCardToHand(stolen);
            events.add(GameEvent.cardStolen(target.getId(), target.getName(), player.getId(), player.getName(), stolen.getType().name()));
            return events;
        }

        return null;
    }

    private List<GameEvent> processCatBalou(GameState state, Player player, Player target, String targetCardId) {
        if (target == null || !target.isAlive()) return null;
        
        List<GameEvent> events = new ArrayList<>();
        
        Card discarded = null;

        // Try to find specific card in play if ID provided
        if (targetCardId != null) {
            discarded = target.getInPlay().stream()
                    .filter(c -> c.getId().equals(targetCardId))
                    .findFirst()
                    .orElse(null);
            
            if (discarded != null) {
                target.getInPlay().remove(discarded);
            } else if (target.getWeapon() != null && target.getWeapon().getId().equals(targetCardId)) {
                discarded = target.getWeapon();
                target.setWeapon(null);
            }
        }

        // If no specific card targeted/found, try hand or fallback logic
        if (discarded == null) {
             if (!target.getHand().isEmpty()) {
                 int index = new Random().nextInt(target.getHand().size());
                 discarded = target.getHand().remove(index);
             } else if (!target.getInPlay().isEmpty()) {
                 discarded = target.getInPlay().remove(0);
             }
        }

        if (discarded != null) {
            state.discardCard(discarded);
            events.add(GameEvent.cardDiscarded(target.getId(), target.getName(), discarded.getType().name(), discarded.getId()));
            return events;
        }

        return null;
    }

    private List<GameEvent> processStagecoach(GameState state, Player player) {
        List<GameEvent> events = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            Card card = state.drawCard();
            if (card != null) {
                player.addCardToHand(card);
                events.add(GameEvent.cardDrawn(player.getId(), player.getName()));
            }
        }
        return events;
    }

    private List<GameEvent> processWellsFargo(GameState state, Player player) {
        List<GameEvent> events = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            Card card = state.drawCard();
            if (card != null) {
                player.addCardToHand(card);
                events.add(GameEvent.cardDrawn(player.getId(), player.getName()));
            }
        }
        return events;
    }

    private List<GameEvent> processDuel(GameState state, Player player, Player target) {
        if (target == null || !target.isAlive()) return null;
        
        state.setPhase(GamePhase.REACTION_PHASE);
        state.getPendingActionPlayers().clear();
        state.getPendingActionPlayers().add(target.getId());
        state.setPendingActionPlayerId(target.getId());
        state.setPendingActionSourcePlayerId(player.getId());
        state.setPendingActionType("DUEL");
        state.setMissedCardsRequired(1);
        
        return new ArrayList<>();
    }

    private List<GameEvent> processGatling(GameState state, Player player) {
        state.getPendingActionPlayers().clear();
        List<Player> alive = state.getAlivePlayers();
        int currentIndex = alive.indexOf(player);

        // Add players after current player
        for (int i = 1; i < alive.size(); i++) {
            int targetIndex = (currentIndex + i) % alive.size();
            state.getPendingActionPlayers().add(alive.get(targetIndex).getId());
        }

        if (!state.getPendingActionPlayers().isEmpty()) {
            state.setPhase(GamePhase.REACTION_PHASE);
            state.setPendingActionPlayerId(state.getPendingActionPlayers().get(0));
            state.setPendingActionSourcePlayerId(player.getId());
            state.setPendingActionType("GATLING");
            state.setMissedCardsRequired(1);
        }
        return new ArrayList<>();
    }

    private List<GameEvent> processIndians(GameState state, Player player) {
        state.getPendingActionPlayers().clear();
        List<Player> alive = state.getAlivePlayers();
        int currentIndex = alive.indexOf(player);

        // Add players after current player
        for (int i = 1; i < alive.size(); i++) {
            int targetIndex = (currentIndex + i) % alive.size();
            state.getPendingActionPlayers().add(alive.get(targetIndex).getId());
        }

        if (!state.getPendingActionPlayers().isEmpty()) {
            state.setPhase(GamePhase.REACTION_PHASE);
            state.setPendingActionPlayerId(state.getPendingActionPlayers().get(0));
            state.setPendingActionSourcePlayerId(player.getId());
            state.setPendingActionType("INDIANS");
            state.setMissedCardsRequired(1);
        }
        return new ArrayList<>();
    }

    private List<GameEvent> processSaloon(GameState state) {
        for (Player p : state.getAlivePlayers()) {
            if (p.getHealth() < p.getMaxHealth()) {
                p.heal(1);
            }
        }
        return new ArrayList<>();
    }

    private List<GameEvent> processGeneralStore(GameState state, Player player) {
        // Draw cards equal to number of alive players
        List<Card> storeCards = new ArrayList<>();
        List<Player> alivePlayers = state.getAlivePlayers();
        for (int i = 0; i < alivePlayers.size(); i++) {
            Card card = state.drawCard();
            if (card != null) {
                storeCards.add(card);
            }
        }
        state.setGeneralStoreCards(storeCards);

        // Setup picking order starting with current player
        state.setPhase(GamePhase.GENERAL_STORE_PHASE);
        state.getPendingActionPlayers().clear();

        int currentIndex = alivePlayers.indexOf(player);
        // Add all players starting from current, wrapping around
        for (int i = 0; i < alivePlayers.size(); i++) {
            int targetIndex = (currentIndex + i) % alivePlayers.size();
            state.getPendingActionPlayers().add(alivePlayers.get(targetIndex).getId());
        }

        if (!state.getPendingActionPlayers().isEmpty()) {
            state.setPendingActionPlayerId(state.getPendingActionPlayers().get(0));
        }

        return new ArrayList<>();
    }

    private List<GameEvent> processJail(GameState state, Player player, Player target) {
        if (target == null || !target.isAlive()) return null;
        if (target.isSheriff()) return null; // Can't jail the Sheriff
        if (target.hasCardInPlay(CardType.JAIL)) return null;
        return new ArrayList<>();
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
        broadcastEvent(roomId, GameEvent.cardDiscarded(player.getId(), player.getName(), card.getType().name(), card.getId()));

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

                    // Broadcast played event for the response card
                    String pendingSourceId = state.getPendingActionSourcePlayerId();
                    Player sourcePlayer = pendingSourceId != null ? state.getPlayerById(pendingSourceId) : null;
                    
                    GameEvent event = GameEvent.cardPlayed(
                            player.getId(), player.getName(),
                            pendingSourceId,
                            sourcePlayer != null ? sourcePlayer.getName() : null,
                            card.getType().name(), card.getId()
                    );
                    broadcastEvent(roomId, event);
                    
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
                            advanceToNextReactionPlayer(state);
                        }
                    }
                }
            }
        } else {
            // Player takes damage
            applyDamage(state, player, 1, state.getPlayerById(state.getPendingActionSourcePlayerId()));
            if ("DUEL".equals(actionType)) {
                clearPendingAction(state);
            } else {
                advanceToNextReactionPlayer(state);
            }
        }

        checkGameEnd(state);
        broadcastGameState(roomId);
    }

    private void applyDamage(GameState state, Player target, int amount, Player source) {
        target.takeDamage(amount);
        
        broadcastEvent(state.getRoomId(), GameEvent.playerDamaged(
                target.getId(), target.getName(), amount, target.getHealth()
        ));

        // Bart Cassidy draws a card when hit
        if (target.getCharacter() == CharacterType.BART_CASSIDY && target.isAlive()) {
            Card drawn = state.drawCard();
            if (drawn != null) {
                target.addCardToHand(drawn);
                broadcastEvent(state.getRoomId(), GameEvent.cardDrawn(target.getId(), target.getName()));
            }
        }

        // El Gringo steals from attacker (only if damaged by a player)
        if (target.getCharacter() == CharacterType.EL_GRINGO && source != null && !source.getHand().isEmpty()) {
            int index = new Random().nextInt(source.getHand().size());
            Card stolen = source.getHand().remove(index);
            target.addCardToHand(stolen);
            broadcastEvent(state.getRoomId(), GameEvent.cardStolen(source.getId(), source.getName(), target.getId(), target.getName(), stolen.getType().name()));
        }

        // Beer Save Check (Last chance)
        if (target.getHealth() <= 0) {
            // Try to use beer from hand automatically?
            // Standard rule: Player can play Beer immediately if lethal damage received
            // This is complex to do "interactive" (ask player), usually implemented as auto-play if available for simplicity in some versions,
            // OR we enter a "DYING" state where they must play Beer.
            // For now, let's auto-play beer if they have it to save them.
            
            Card beer = target.getHand().stream()
                    .filter(c -> c.getType() == CardType.BEER)
                    .findFirst().orElse(null);
            
            while (target.getHealth() <= 0 && beer != null) {
                target.removeCardFromHand(beer);
                state.discardCard(beer);
                target.heal(1);
                
                broadcastEvent(state.getRoomId(), GameEvent.cardPlayed(
                        target.getId(), target.getName(),
                        null, null,
                        "BEER", beer.getId()
                ));
                
                beer = target.getHand().stream()
                        .filter(c -> c.getType() == CardType.BEER)
                        .findFirst().orElse(null);
            }
        }

        if (!target.isAlive()) {
            handlePlayerDeath(state, target, source);
        }
    }

    private void handlePlayerDeath(GameState state, Player eliminated, Player killer) {
        broadcastEvent(state.getRoomId(), GameEvent.playerEliminated(
                eliminated.getId(), eliminated.getName(), eliminated.getRole().name()
        ));

        // Vulture Sam takes all cards
        for (Player p : state.getAlivePlayers()) {
            if (p.getCharacter() == CharacterType.VULTURE_SAM) {
                // Vulture Sam takes all cards
                int cardCount = eliminated.getHand().size() + eliminated.getInPlay().size() + (eliminated.getWeapon() != null ? 1 : 0);
                
                p.getHand().addAll(eliminated.getHand());
                p.getHand().addAll(eliminated.getInPlay());
                if (eliminated.getWeapon() != null) {
                    p.getHand().add(eliminated.getWeapon());
                }
                
                for(int i=0; i<cardCount; i++) {
                     broadcastEvent(state.getRoomId(), GameEvent.cardStolen(eliminated.getId(), eliminated.getName(), p.getId(), p.getName(), "UNKNOWN"));
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
                    if (card != null) {
                        killer.addCardToHand(card);
                        broadcastEvent(state.getRoomId(), GameEvent.cardDrawn(killer.getId(), killer.getName()));
                    }
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
    }

    private void clearPendingAction(GameState state) {
        state.setPendingActionPlayerId(null);
        state.getPendingActionPlayers().clear();
        state.setPendingActionSourcePlayerId(null);
        state.setPendingActionType(null);
        state.setPendingActionCard(null);
        state.setMissedCardsRequired(0);
        state.getUsedReactionAbilities().clear();
        state.setPhase(GamePhase.PLAY_PHASE);
    }

    private void advanceToNextReactionPlayer(GameState state) {
        String current = state.getPendingActionPlayerId();
        state.getPendingActionPlayers().remove(current);
        state.getUsedReactionAbilities().clear();

        if (!state.getPendingActionPlayers().isEmpty()) {
            state.setPendingActionPlayerId(state.getPendingActionPlayers().get(0));
            state.setMissedCardsRequired(1);
        } else {
            clearPendingAction(state);
        }
    }

    public void pickGeneralStoreCard(String roomId, String playerId, String cardId) {
        GameState state = games.get(roomId);
        if (state == null) return;
        if (state.getPhase() != GamePhase.GENERAL_STORE_PHASE) return;

        if (!playerId.equals(state.getPendingActionPlayerId())) return;

        Card pickedCard = state.getGeneralStoreCards().stream()
                .filter(c -> c.getId().equals(cardId))
                .findFirst()
                .orElse(null);

        if (pickedCard == null) return;

        Player player = state.getPlayerById(playerId);
        if (player == null) return;

        // Add to hand
        player.addCardToHand(pickedCard);
        state.getGeneralStoreCards().remove(pickedCard);

        broadcastEvent(roomId, GameEvent.cardDrawn(player.getId(), player.getName()));

        // Next player
        state.getPendingActionPlayers().remove(playerId);

        if (state.getPendingActionPlayers().isEmpty() || state.getGeneralStoreCards().isEmpty()) {
            // End of General Store
            state.getPendingActionPlayers().clear();
            state.setPendingActionPlayerId(null);
            state.getGeneralStoreCards().clear();
            state.setPhase(GamePhase.PLAY_PHASE);
        } else {
            state.setPendingActionPlayerId(state.getPendingActionPlayers().get(0));
        }

        broadcastGameState(roomId);
    }

    public void useAbility(String roomId, String playerId, String abilityId) {
        GameState state = games.get(roomId);
        if (state == null) return;
        if (state.getPhase() != GamePhase.REACTION_PHASE) return;

        // Barrel/Ability only works for BANG and GATLING
        if (!"BANG".equals(state.getPendingActionType()) && !"GATLING".equals(state.getPendingActionType())) return;

        Player player = state.getPlayerById(playerId);
        if (player == null || !player.getId().equals(state.getPendingActionPlayerId())) {
            return;
        }

        // Check if already used
        if (state.getUsedReactionAbilities().contains(abilityId)) return;

        // Verify player has this ability
        boolean hasAbility = false;
        if ("JOURDONNAIS".equals(abilityId)) {
            hasAbility = player.getCharacter() == CharacterType.JOURDONNAIS;
        } else {
            // Check in-play cards (Barrel)
            String finalAbilityId = abilityId;
            hasAbility = player.getInPlay().stream()
                    .anyMatch(c -> c.getId().equals(finalAbilityId) && c.getType() == CardType.BARREL);
        }

        if (!hasAbility) return;
        
        // Draw card for check
        Card checkCard = state.drawCard();
        if (checkCard != null) {
            state.discardCard(checkCard);
            
            // Create check event with extra data for display
            GameEvent checkEvent = GameEvent.cardCheck(player.getId(), player.getName(), checkCard.getType().name(), checkCard.getId());
            // Add suit and value info to data payload for frontend
            checkEvent.setData(Map.of(
                "suit", checkCard.getSuit().name(),
                "value", checkCard.getValue()
            ));
            broadcastEvent(roomId, checkEvent);
            
            boolean success = checkCard.getSuit() == CardSuit.HEARTS;
            
            // Lucky Duke: draw second card and choose best
            if (player.getCharacter() == CharacterType.LUCKY_DUKE) {
                Card checkCard2 = state.drawCard();
                if (checkCard2 != null) {
                     state.discardCard(checkCard2);
                     
                     GameEvent checkEvent2 = GameEvent.cardCheck(player.getId(), player.getName(), checkCard2.getType().name(), checkCard2.getId());
                     checkEvent2.setData(Map.of(
                        "suit", checkCard2.getSuit().name(),
                        "value", checkCard2.getValue()
                     ));
                     broadcastEvent(roomId, checkEvent2);
                     
                     if (checkCard2.getSuit() == CardSuit.HEARTS) {
                         success = true;
                     }
                }
            }

            if (success) {
                 int remaining = state.getMissedCardsRequired() - 1;
                 state.setMissedCardsRequired(remaining);

                 if (remaining <= 0) {
                     advanceToNextReactionPlayer(state);
                 }
            } else {
                state.getUsedReactionAbilities().add(abilityId);
            }
            
            broadcastGameState(roomId);
        }
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
        // Check for Dynamite - Priority BEFORE Jail
        Card dynamite = player.getInPlay().stream()
                .filter(c -> c.getType() == CardType.DYNAMITE)
                .findFirst().orElse(null);

        if (dynamite != null) {
            // Draw for dynamite check
            Card drawn = state.drawCard();
            if (drawn != null) {
                state.discardCard(drawn);
                
                // Broadcast check event
                GameEvent checkEvent = GameEvent.cardCheck(player.getId(), player.getName(), drawn.getType().name(), drawn.getId());
                checkEvent.setData(Map.of(
                    "suit", drawn.getSuit().name(),
                    "value", drawn.getValue()
                ));
                broadcastEvent(state.getRoomId(), checkEvent);

                // Check for explosion (Spades 2-9)
                boolean explode = drawn.getSuit() == CardSuit.SPADES &&
                                  "23456789".contains(drawn.getValue());
                // Handle 10 specifically if needed, but standard BANG deck usually has 2-9 Spades for Dynamite
                // Re-verify specific card values if needed, but usually 2-9 of Spades triggers it.
                // My previous code had "10" in the string check which was a bit weird with replace.
                // Standard rule: Spades 2 through 9.
                
                if (explode) {
                    // Dynamite explodes - 3 damage
                    player.getInPlay().remove(dynamite);
                    state.discardCard(dynamite);
                    broadcastEvent(state.getRoomId(), GameEvent.cardDiscarded(player.getId(), player.getName(), dynamite.getType().name(), dynamite.getId()));
                    
                    applyDamage(state, player, 3, null);
                    
                    if (!player.isAlive()) {
                        // If player died from dynamite, turn ends immediately (handled in handlePlayerDeath/checkGameEnd usually,
                        // but we should ensure we don't process Jail if dead)
                        return;
                    }
                } else {
                    // Pass dynamite to next player
                    player.getInPlay().remove(dynamite);
                    Player next = getNextAlivePlayer(state, player);
                    if (next != null) {
                        next.getInPlay().add(dynamite);
                        // Broadcast PASS event
                        broadcastEvent(state.getRoomId(), GameEvent.cardPassed(
                            player.getId(), player.getName(),
                            next.getId(), next.getName(),
                            dynamite.getType().name(), dynamite.getId()
                        ));
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

                GameEvent checkEvent = GameEvent.cardCheck(player.getId(), player.getName(), drawn.getType().name(), drawn.getId());
                checkEvent.setData(Map.of(
                        "suit", drawn.getSuit().name(),
                        "value", drawn.getValue()
                ));
                broadcastEvent(state.getRoomId(), checkEvent);

                // Broadcast Jail discard AFTER check
                broadcastEvent(state.getRoomId(), GameEvent.cardDiscarded(player.getId(), player.getName(), jail.getType().name(), jail.getId()));

                if (drawn.getSuit() != CardSuit.HEARTS) {
                    // Stays in jail - skip turn
                    endTurn(state);
                    return;
                }
            } else {
                broadcastEvent(state.getRoomId(), GameEvent.cardDiscarded(player.getId(), player.getName(), jail.getType().name(), jail.getId()));
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

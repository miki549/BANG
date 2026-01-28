package com.example.bang.service;

import com.example.bang.model.*;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Component
public class DeckBuilder {

    public List<Card> createDeck() {
        List<Card> deck = new ArrayList<>();

        // BANG! cards (25 total)
        addCards(deck, CardType.BANG, CardColor.BROWN, "bang", new String[][]{
                {"A", "SPADES"}, {"2", "DIAMONDS"}, {"3", "DIAMONDS"}, {"4", "DIAMONDS"},
                {"5", "DIAMONDS"}, {"6", "DIAMONDS"}, {"7", "DIAMONDS"}, {"8", "DIAMONDS"},
                {"9", "DIAMONDS"}, {"10", "DIAMONDS"}, {"J", "DIAMONDS"}, {"Q", "DIAMONDS"},
                {"K", "DIAMONDS"}, {"A", "DIAMONDS"}, {"Q", "HEARTS"}, {"K", "HEARTS"},
                {"A", "HEARTS"}, {"2", "CLUBS"}, {"3", "CLUBS"}, {"4", "CLUBS"},
                {"5", "CLUBS"}, {"6", "CLUBS"}, {"7", "CLUBS"}, {"8", "CLUBS"}, {"9", "CLUBS"}
        });

        // Missed! cards (12 total)
        addCards(deck, CardType.MISSED, CardColor.BROWN, "missed", new String[][]{
                {"10", "CLUBS"}, {"J", "CLUBS"}, {"Q", "CLUBS"}, {"K", "CLUBS"},
                {"A", "CLUBS"}, {"2", "SPADES"}, {"3", "SPADES"}, {"4", "SPADES"},
                {"5", "SPADES"}, {"6", "SPADES"}, {"7", "SPADES"}, {"8", "SPADES"}
        });

        // Beer cards (6 total)
        addCards(deck, CardType.BEER, CardColor.BROWN, "beer", new String[][]{
                {"6", "HEARTS"}, {"7", "HEARTS"}, {"8", "HEARTS"}, {"9", "HEARTS"},
                {"10", "HEARTS"}, {"J", "HEARTS"}
        });

        // Panic! cards (4 total)
        addCards(deck, CardType.PANIC, CardColor.BROWN, "panic", new String[][]{
                {"J", "HEARTS"}, {"Q", "HEARTS"}, {"A", "HEARTS"}, {"8", "DIAMONDS"}
        });

        // Cat Balou cards (4 total)
        addCards(deck, CardType.CAT_BALOU, CardColor.BROWN, "cat_balou", new String[][]{
                {"K", "HEARTS"}, {"9", "DIAMONDS"}, {"10", "DIAMONDS"}, {"J", "DIAMONDS"}
        });

        // Stagecoach (2 total)
        addCards(deck, CardType.STAGECOACH, CardColor.BROWN, "stagecoach", new String[][]{
                {"9", "SPADES"}, {"9", "SPADES"}
        });

        // Wells Fargo (1 total)
        addCards(deck, CardType.WELLS_FARGO, CardColor.BROWN, "wells_fargo", new String[][]{
                {"3", "HEARTS"}
        });

        // Gatling (1 total)
        addCards(deck, CardType.GATLING, CardColor.BROWN, "gatling", new String[][]{
                {"10", "HEARTS"}
        });

        // Duel (3 total)
        addCards(deck, CardType.DUEL, CardColor.BROWN, "duel", new String[][]{
                {"Q", "DIAMONDS"}, {"J", "SPADES"}, {"8", "CLUBS"}
        });

        // Indians! (2 total)
        addCards(deck, CardType.INDIANS, CardColor.BROWN, "indians", new String[][]{
                {"K", "DIAMONDS"}, {"A", "DIAMONDS"}
        });

        // General Store (2 total)
        addCards(deck, CardType.GENERAL_STORE, CardColor.BROWN, "general_store", new String[][]{
                {"9", "CLUBS"}, {"Q", "SPADES"}
        });

        // Saloon (1 total)
        addCards(deck, CardType.SALOON, CardColor.BROWN, "saloon", new String[][]{
                {"5", "HEARTS"}
        });

        // Barrel (2 total)
        addCards(deck, CardType.BARREL, CardColor.BLUE, "barrel", new String[][]{
                {"Q", "SPADES"}, {"K", "SPADES"}
        });

        // Scope (1 total)
        addCards(deck, CardType.SCOPE, CardColor.BLUE, "scope", new String[][]{
                {"A", "SPADES"}
        });

        // Mustang (2 total)
        addCards(deck, CardType.MUSTANG, CardColor.BLUE, "mustang", new String[][]{
                {"8", "HEARTS"}, {"9", "HEARTS"}
        });

        // Jail (3 total)
        addCards(deck, CardType.JAIL, CardColor.BLUE, "jail", new String[][]{
                {"J", "SPADES"}, {"4", "HEARTS"}, {"10", "SPADES"}
        });

        // Dynamite (1 total)
        addCards(deck, CardType.DYNAMITE, CardColor.BLUE, "dynamite", new String[][]{
                {"2", "HEARTS"}
        });

        // Volcanic (2 total)
        addCards(deck, CardType.VOLCANIC, CardColor.BLUE, "volcanic", new String[][]{
                {"10", "SPADES"}, {"10", "CLUBS"}
        });

        // Schofield (3 total)
        addCards(deck, CardType.SCHOFIELD, CardColor.BLUE, "schofield", new String[][]{
                {"J", "CLUBS"}, {"Q", "CLUBS"}, {"K", "SPADES"}
        });

        // Remington (1 total)
        addCards(deck, CardType.REMINGTON, CardColor.BLUE, "remington", new String[][]{
                {"K", "CLUBS"}
        });

        // Rev. Carabine (1 total)
        addCards(deck, CardType.REV_CARABINE, CardColor.BLUE, "rev_carabine", new String[][]{
                {"A", "CLUBS"}
        });

        // Winchester (1 total)
        addCards(deck, CardType.WINCHESTER, CardColor.BLUE, "winchester", new String[][]{
                {"8", "SPADES"}
        });

        Collections.shuffle(deck);
        return deck;
    }

    private void addCards(List<Card> deck, CardType type, CardColor color, String imageName, String[][] cardData) {
        for (String[] data : cardData) {
            deck.add(Card.builder()
                    .id(UUID.randomUUID().toString())
                    .type(type)
                    .suit(CardSuit.valueOf(data[1]))
                    .value(data[0])
                    .color(color)
                    .imageName(imageName)
                    .build());
        }
    }

    public List<Role> getRolesForPlayerCount(int playerCount) {
        List<Role> roles = new ArrayList<>();
        roles.add(Role.SHERIFF);
        roles.add(Role.RENEGADE);

        switch (playerCount) {
            case 4:
                roles.add(Role.OUTLAW);
                roles.add(Role.OUTLAW);
                break;
            case 5:
                roles.add(Role.OUTLAW);
                roles.add(Role.OUTLAW);
                roles.add(Role.DEPUTY);
                break;
            case 6:
                roles.add(Role.OUTLAW);
                roles.add(Role.OUTLAW);
                roles.add(Role.OUTLAW);
                roles.add(Role.DEPUTY);
                break;
            case 7:
                roles.add(Role.OUTLAW);
                roles.add(Role.OUTLAW);
                roles.add(Role.OUTLAW);
                roles.add(Role.DEPUTY);
                roles.add(Role.DEPUTY);
                break;
            default:
                throw new IllegalArgumentException("Player count must be between 4 and 7");
        }

        Collections.shuffle(roles);
        return roles;
    }

    public List<CharacterType> getRandomCharacters(int count) {
        List<CharacterType> allCharacters = new ArrayList<>(List.of(CharacterType.values()));
        Collections.shuffle(allCharacters);
        return allCharacters.subList(0, Math.min(count, allCharacters.size()));
    }
}

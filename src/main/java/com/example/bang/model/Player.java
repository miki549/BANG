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
public class Player {
    private String id;
    private String sessionId;
    private String name;
    private Role role;
    private CharacterType character;
    private int health;
    private int maxHealth;
    private boolean alive;
    private boolean isSheriff;

    @Builder.Default
    private List<Card> hand = new ArrayList<>();
    
    @Builder.Default
    private List<Card> inPlay = new ArrayList<>();
    
    private Card weapon;
    private int seatPosition;

    @Builder.Default
    private int bangsPlayedThisTurn = 0;

    public int getHandLimit() {
        return health;
    }

    public boolean canPlayBang() {
        if (character == CharacterType.WILLY_THE_KID) {
            return true;
        }
        if (weapon != null && weapon.getType() == CardType.VOLCANIC) {
            return true;
        }
        return bangsPlayedThisTurn < 1;
    }

    public int getWeaponRange() {
        if (weapon != null) {
            return weapon.getWeaponRange();
        }
        return 1; // Default Colt .45 range
    }

    public boolean hasCardInPlay(CardType type) {
        return inPlay.stream().anyMatch(c -> c.getType() == type);
    }

    public boolean hasBarrelEffect() {
        if (character == CharacterType.JOURDONNAIS) {
            return true;
        }
        return hasCardInPlay(CardType.BARREL);
    }

    public boolean hasMustangEffect() {
        if (character == CharacterType.PAUL_REGRET) {
            return true;
        }
        return hasCardInPlay(CardType.MUSTANG);
    }

    public boolean hasScopeEffect() {
        if (character == CharacterType.ROSE_DOOLAN) {
            return true;
        }
        return hasCardInPlay(CardType.SCOPE);
    }

    public int getDistanceModifierIncoming() {
        int modifier = 0;
        if (hasMustangEffect()) modifier++;
        if (hasCardInPlay(CardType.MUSTANG) && character == CharacterType.PAUL_REGRET) modifier++;
        return modifier;
    }

    public int getDistanceModifierOutgoing() {
        int modifier = 0;
        if (hasScopeEffect()) modifier--;
        if (hasCardInPlay(CardType.SCOPE) && character == CharacterType.ROSE_DOOLAN) modifier--;
        return modifier;
    }

    public void takeDamage(int amount) {
        this.health = Math.max(0, this.health - amount);
        if (this.health <= 0) {
            this.alive = false;
        }
    }

    public void heal(int amount) {
        this.health = Math.min(this.maxHealth, this.health + amount);
    }

    public void addCardToHand(Card card) {
        this.hand.add(card);
    }

    public void removeCardFromHand(Card card) {
        this.hand.remove(card);
    }

    public Card findCardInHand(String cardId) {
        return hand.stream()
                .filter(c -> c.getId().equals(cardId))
                .findFirst()
                .orElse(null);
    }

    public void resetTurn() {
        this.bangsPlayedThisTurn = 0;
    }
}

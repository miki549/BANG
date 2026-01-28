package com.example.bang.model;

public enum CharacterType {
    BART_CASSIDY("Bart Cassidy", 4, "Each time he loses a life point, he immediately draws a card from the deck."),
    BLACK_JACK("Black Jack", 4, "During draw phase, he must show the second card: if it's Hearts or Diamonds, he draws one additional card."),
    CALAMITY_JANET("Calamity Janet", 4, "She can use BANG! cards as Missed! cards and vice versa."),
    EL_GRINGO("El Gringo", 3, "Each time he loses a life point due to a card played by another player, he draws a random card from that player's hand."),
    JESSE_JONES("Jesse Jones", 4, "During draw phase, he may draw the first card from a player's hand instead of the deck."),
    JOURDONNAIS("Jourdonnais", 4, "He is considered to have a Barrel in play at all times."),
    KIT_CARLSON("Kit Carlson", 4, "During draw phase, he looks at the top 3 cards and chooses 2, putting the other back."),
    LUCKY_DUKE("Lucky Duke", 4, "Each time he is required to 'draw!', he flips two cards and chooses the result."),
    PAUL_REGRET("Paul Regret", 3, "He is considered to have a Mustang in play at all times."),
    PEDRO_RAMIREZ("Pedro Ramirez", 4, "During draw phase, he may draw the first card from the discard pile instead of the deck."),
    ROSE_DOOLAN("Rose Doolan", 4, "She is considered to have a Scope in play at all times."),
    SID_KETCHUM("Sid Ketchum", 4, "He may discard 2 cards from his hand to regain one life point at any time."),
    SLAB_THE_KILLER("Slab the Killer", 4, "Players need to play 2 Missed! cards to cancel his BANG!"),
    SUZY_LAFAYETTE("Suzy Lafayette", 4, "As soon as she has no cards in hand, she draws a card from the deck."),
    VULTURE_SAM("Vulture Sam", 4, "When a character is eliminated, Sam takes all their cards."),
    WILLY_THE_KID("Willy the Kid", 4, "He can play any number of BANG! cards during his turn.");

    private final String displayName;
    private final int maxHealth;
    private final String ability;

    CharacterType(String displayName, int maxHealth, String ability) {
        this.displayName = displayName;
        this.maxHealth = maxHealth;
        this.ability = ability;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getMaxHealth() {
        return maxHealth;
    }

    public String getAbility() {
        return ability;
    }
}

package com.example.bang.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Card {
    private String id;
    private CardType type;
    private CardSuit suit;
    private String value; // A, 2-10, J, Q, K
    private CardColor color;
    private String imageName;

    public int getWeaponRange() {
        return switch (type) {
            case VOLCANIC -> 1;
            case SCHOFIELD -> 2;
            case REMINGTON -> 3;
            case REV_CARABINE -> 4;
            case WINCHESTER -> 5;
            default -> 0;
        };
    }

    public boolean isWeapon() {
        return type == CardType.VOLCANIC || type == CardType.SCHOFIELD ||
               type == CardType.REMINGTON || type == CardType.REV_CARABINE ||
               type == CardType.WINCHESTER;
    }

    public boolean isBrownCard() {
        return color == CardColor.BROWN;
    }

    public boolean isBlueCard() {
        return color == CardColor.BLUE;
    }

    public String getDisplayName() {
        return switch (type) {
            case BANG -> "BANG!";
            case MISSED -> "Missed!";
            case BEER -> "Beer";
            case SALOON -> "Saloon";
            case STAGECOACH -> "Stagecoach";
            case WELLS_FARGO -> "Wells Fargo";
            case PANIC -> "Panic!";
            case CAT_BALOU -> "Cat Balou";
            case DUEL -> "Duel";
            case GATLING -> "Gatling";
            case INDIANS -> "Indians!";
            case GENERAL_STORE -> "General Store";
            case BARREL -> "Barrel";
            case MUSTANG -> "Mustang";
            case SCOPE -> "Scope";
            case JAIL -> "Jail";
            case DYNAMITE -> "Dynamite";
            case VOLCANIC -> "Volcanic";
            case SCHOFIELD -> "Schofield";
            case REMINGTON -> "Remington";
            case REV_CARABINE -> "Rev. Carabine";
            case WINCHESTER -> "Winchester";
        };
    }
}

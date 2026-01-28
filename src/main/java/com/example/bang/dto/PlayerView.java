package com.example.bang.dto;

import com.example.bang.model.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlayerView {
    private String id;
    private String name;
    private String characterName;
    private String characterAbility;
    private int health;
    private int maxHealth;
    private boolean alive;
    private boolean isSheriff;
    private int handSize;
    private List<Card> cardsInPlay;
    private Card weapon;
    private int seatPosition;
    private Role role; // Only visible if sheriff or if viewing own role
    private List<Card> hand; // Only included for the requesting player

    public static PlayerView fromPlayer(Player player, boolean includeHand, boolean includeRole) {
        return PlayerView.builder()
                .id(player.getId())
                .name(player.getName())
                .characterName(player.getCharacter().getDisplayName())
                .characterAbility(player.getCharacter().getAbility())
                .health(player.getHealth())
                .maxHealth(player.getMaxHealth())
                .alive(player.isAlive())
                .isSheriff(player.isSheriff())
                .handSize(player.getHand().size())
                .cardsInPlay(player.getInPlay())
                .weapon(player.getWeapon())
                .seatPosition(player.getSeatPosition())
                .role(includeRole ? player.getRole() : null)
                .hand(includeHand ? player.getHand() : null)
                .build();
    }
}

package com.example.bang.model;

public enum GamePhase {
    WAITING_FOR_PLAYERS,  // In lobby
    STARTING,             // Dealing cards
    DRAW_PHASE,           // Player draws 2 cards
    PLAY_PHASE,           // Player can play cards
    DISCARD_PHASE,        // Player must discard excess cards
    REACTION_PHASE,       // Waiting for target to respond (e.g., BANG -> MISSED)
    GENERAL_STORE_PHASE,  // Players choosing cards from General Store
    GAME_OVER             // Game ended
}

package com.example.bang.model;

public enum Role {
    SHERIFF("Sheriff", "Eliminate all Outlaws and the Renegade"),
    DEPUTY("Deputy", "Protect the Sheriff at all costs"),
    OUTLAW("Outlaw", "Kill the Sheriff"),
    RENEGADE("Renegade", "Be the last one standing");

    private final String displayName;
    private final String objective;

    Role(String displayName, String objective) {
        this.displayName = displayName;
        this.objective = objective;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getObjective() {
        return objective;
    }
}

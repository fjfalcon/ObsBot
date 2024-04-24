package com.fjfalcon.inetmafia.model;

import java.util.List;

public class Players {
    private int max;
    private List<Player> players;

    // getters and setters
    public int getMax() {
        return max;
    }

    public void setMax(int max) {
        this.max = max;
    }

    public List<Player> getPlayers() {
        return players;
    }

    public void setPlayers(List<Player> players) {
        this.players = players;
    }
}

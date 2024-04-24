package com.fjfalcon.inetmafia.model;

public class Lobby {
    private int id;
    private String title;
    private boolean isLobby;
    private Players players;
    private String spectators;
    private String __typename;

    // getters and setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public boolean getIsLobby() {
        return isLobby;
    }

    public void setIsLobby(boolean isLobby) {
        this.isLobby = isLobby;
    }

    public Players getPlayers() {
        return players;
    }

    public void setPlayers(Players players) {
        this.players = players;
    }

    public String getSpectators() {
        return spectators;
    }

    public void setSpectators(String spectators) {
        this.spectators = spectators;
    }

    public String get__typename() {
        return __typename;
    }

    public void set__typename(String __typename) {
        this.__typename = __typename;
    }
}

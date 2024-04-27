package com.fjfalcon.inetmafia.client;

import com.fjfalcon.inetmafia.model.Player;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.stream.Collectors;

@Service
public class InetMafiaService {
    private final InetMafiaClient inetMafiaClient;

    public InetMafiaService(InetMafiaClient inetMafiaClient) {
        this.inetMafiaClient = inetMafiaClient;
    }

    public boolean isGameIsRunning(String id) {
        int lobbyId = Integer.parseInt(id);
        return inetMafiaClient.getLobbies().stream().anyMatch(it -> it.getId() == lobbyId && !it.getIsLobby());
    }

    public String getPlayers(String id) {
        int lobbyId = Integer.parseInt(id);
        return inetMafiaClient.getLobbies().stream().filter(it -> it.getId() == lobbyId).findAny().map(it -> it.getPlayers().getPlayers().stream().sorted(Comparator.comparing(Player::getPosition)).map(Player::toString).collect(Collectors.joining("\n"))).orElse("Игроков не найдено, или игры не найдено, или ты просто не нравишься мне.");
    }

    public void updateLobby() {
        inetMafiaClient.updateLobby();
    }
}

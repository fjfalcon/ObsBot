package com.fjfalcon.inetmafia.client;

import com.fjfalcon.inetmafia.model.Lobby;
import com.fjfalcon.inetmafia.model.LobbyData;
import com.fjfalcon.inetmafia.model.Root;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Component
public class InetMafiaClient {
    private static final String INET_MAFIA_URL = "https://inetmafia.ru/api";
    private static final String GET_LOBBY_QUERY = "{\"query\":\"query { lobby { id title isLobby players spectators __typename }  }\"}";

    private final Logger logger = LoggerFactory.getLogger(InetMafiaClient.class);

    private volatile List<Lobby> lobbies;

    private final RestTemplate restTemplate;

    public InetMafiaClient() {
        this.restTemplate = new RestTemplate();
        updateLobby();
    }

    public List<Lobby> getLobbies() {
        return lobbies;
    }

    private List<Lobby> getLobby() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<String> request = new HttpEntity<>(GET_LOBBY_QUERY, headers);
        return Optional.of(restTemplate.postForEntity(INET_MAFIA_URL, request, Root.class)).map(HttpEntity::getBody).map(Root::getData).map(LobbyData::getLobby).orElse(Collections.emptyList());
    }

    @Scheduled(fixedDelay = 60  * 1000)
    public void updateLobby() {
        try {
            var lobby = getLobby();
            if (lobby != null && !lobby.isEmpty()) {
                lobbies = lobby;
            }
        } catch (Exception e) {
            logger.error("Failed to update lobby data", e);
        }
    }
}

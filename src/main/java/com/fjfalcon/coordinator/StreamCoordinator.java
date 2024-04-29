package com.fjfalcon.coordinator;

import com.fjfalcon.inetmafia.client.InetMafiaService;
import com.fjfalcon.inetmafia.model.Lobby;
import com.fjfalcon.obs.ObsController;
import com.fjfalcon.telegram.ObsPoll;
import com.fjfalcon.youtube.YoutubeClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Component
public class StreamCoordinator {
    private final Logger logger = LoggerFactory.getLogger(StreamCoordinator.class);
    public static final String GAME_URL = "https://inetmafia.ru/game";
    private final ObsController obsController;
    private final InetMafiaService inetMafiaService;
    private final ScheduledExecutorService scheduledExecutorService;
    private final YoutubeClient youtubeClient;
    private final ObsPoll obsPoll;
    private CoordinatorMode mode = CoordinatorMode.SET_MODE;
    private boolean isTurnOffScheduled = false;
    private boolean isStreamSnippingScheduled = false;
    private String nickname = "";
    private boolean finderLaunched = false;

    public StreamCoordinator(ObsController obsController, InetMafiaService inetMafiaService, YoutubeClient youtubeClient, @Lazy ObsPoll obsPoll) {
        this.obsController = obsController;
        this.inetMafiaService = inetMafiaService;
        this.youtubeClient = youtubeClient;
        this.obsPoll = obsPoll;
        scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
    }

    public String getBrowserUrl() {
        return obsController.getUrl();
    }

    public boolean isGameRunning(String id) {
        inetMafiaService.updateLobby();
        return inetMafiaService.isGameIsRunning(id);
    }

    private boolean isGameNotRunning() {
        var url = getBrowserUrl();
        if (url.contains(GAME_URL)) {
            var gameId = url.substring(GAME_URL.length() + 1);
            return !isGameRunning(gameId);
        }
        return true;
    }

    public void resetStreamCoordinator() {
        isStreamSnippingScheduled = false;
        isTurnOffScheduled = false;
        finderLaunched = false;
    }

    @Scheduled(fixedDelay = 1000 * 60)
    public void checkStreamState() {
        if (isGameNotRunning()) {
            logger.info("Игры в стриме нет");
            if (obsController.isStreamEnabled()) {
                if (!isTurnOffScheduled) {
                    scheduledExecutorService.schedule(new StopStreamIfNoOneUsingIt(), 20, TimeUnit.MINUTES);
                    isTurnOffScheduled = true;
                }
            }

            if (mode == CoordinatorMode.FOLLOW_MODE) {
                if (!finderLaunched) {
                    finderLaunched = true;
                    findLobbyForPlayer();
                }
            }
        } else
            isTurnOffScheduled = false;
    }

    public void disableFollowMode() {
        nickname = "";
        mode = CoordinatorMode.SET_MODE;
    }

    public String setFollowMode(String nick) {
        mode = CoordinatorMode.FOLLOW_MODE;
        nickname = nick;
        findLobbyForPlayer();
        return "Бот переведен в режим follow для игрока " + nick;
    }

    private class StopStreamIfNoOneUsingIt implements Runnable {
        @Override
        public void run() {
            if (obsController.isStreamEnabled()) {
                if (isGameNotRunning()) {
                    youtubeClient.stopStreaming();
                    obsController.stopStreaming();
                    logger.info("Turning off stream");
                    obsPoll.sendTextToMe("Stream is stopped");
                    isTurnOffScheduled = false;
                }
            }
        }
    }

    public String getKeys() {
        return "isTurnOffScheduled " + isTurnOffScheduled + " isStreamSnippingScheduled " + isStreamSnippingScheduled + " nickname " + nickname + " finderLaunched " + finderLaunched + " mode " + mode;
    }

    public void findLobbyForPlayer() {
        logger.info("Я в поиске лобби для игрока {} ", nickname);
        inetMafiaService.updateLobby();
        var lobbies = inetMafiaService.getLobby().stream().filter(lobby -> lobby.getPlayers().getPlayers().stream().anyMatch(player -> nickname.equals(player.getNick()))).toList();

        if (lobbies.isEmpty()) {
            finderLaunched = false;
            logger.info("Лобби не найдено для игрока {}, возвращаюсь в поиск", nickname);
            return;
        }
        logger.info("Лобби найдено для игрока {}", nickname);

        if (lobbies.size() == 1) {
            prepareStreamForLobby(lobbies.getFirst());
        } else {
            prepareStreamForLobby(lobbies.stream().filter(Lobby::getIsLobby).findFirst().orElse(lobbies.stream().filter(l -> l.getPlayers().getPlayers().stream().anyMatch(player -> nickname.equals(player.getNick()) && !player.isDead())).findFirst().get()));

        }
    }

    private void prepareStreamForLobby(Lobby lobby) {
        lobby = inetMafiaService.updateLobby(lobby);
        if (lobby == null || lobby.getPlayers().getPlayers().stream().noneMatch(player -> player.getNick().equals(nickname))) {
            finderLaunched = false;
            return;
        }
        logger.info("Жду лобби {} для игрока {}", lobby.getId(), nickname);
        if (lobby.getIsLobby()) {
            int sleep = generateSleepBasedOnNumberOfPlayers(lobby.getPlayers().getPlayers().size());
            if (!isStreamSnippingScheduled) {
                scheduledExecutorService.schedule(new PrepareStreamForLobby(lobby), sleep, TimeUnit.SECONDS);
                isStreamSnippingScheduled = true;
            }
        } else {
            if (!lobby.getSpectators().equals("5")) {
                joinStream(lobby.getId());
            } else {
                logger.info("Лобби {} заполнено", lobby.getId());
                scheduledExecutorService.schedule(new PrepareStreamForLobby(lobby), 5, TimeUnit.SECONDS);
            }
        }
    }

    private void joinStream(int id) {
        if (isGameNotRunning()) {
            obsController.setUrl(id + "");
            if (!obsController.isStreamEnabled()) {
                youtubeClient.checkStreamStarted();
            }

            obsPoll.sendTextToMe("Стрим присоединился к игре " + id);
            logger.info("Стрим присоединился к игре {}", id);
            finderLaunched = false;
        }
    }

    private int generateSleepBasedOnNumberOfPlayers(int size) {
        if (size <= 5) {
            return 20;
        } else if (size <= 9) {
            return 15;
        } else if (size == 10){
            return 3;
        } else {
            return 1;
        }
    }

    private class PrepareStreamForLobby implements Runnable {
        private final Lobby lobby;

        private PrepareStreamForLobby(Lobby lobby) {
            this.lobby = lobby;
        }

        @Override
        public void run() {
            isStreamSnippingScheduled = false;
            prepareStreamForLobby(lobby);

        }
    }
}

package com.fjfalcon.coordinator;

import com.fjfalcon.inetmafia.client.InetMafiaService;
import com.fjfalcon.obs.ObsController;
import com.fjfalcon.telegram.ObsPoll;
import com.fjfalcon.youtube.YoutubeClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    private CoordinatorMode mode;
    private boolean isTurnOffScheduled = false;

    public StreamCoordinator(ObsController obsController, InetMafiaService inetMafiaService, YoutubeClient youtubeClient, ObsPoll obsPoll) {
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

    @Scheduled(fixedDelay = 1000 * 60)
    public void checkStreamState() {
        if (isGameNotRunning()) {
            if (obsController.isStreamEnabled()) {
                if (!isTurnOffScheduled) {
                    scheduledExecutorService.schedule(new Runnable() {
                        @Override
                        public void run() {
                            if (obsController.isStreamEnabled()) {
                                if (isGameNotRunning()) {
                                    youtubeClient.stopStreaming();
                                    obsController.stopStreaming();
                                    logger.info("Turning off stream");
                                    obsPoll.sendTextToMe("Stream is stopped");
                                }
                            }
                        }
                    }, 10, TimeUnit.MINUTES);
                    isTurnOffScheduled = true;
                }
            }
        } else
            isTurnOffScheduled = false;
    }
}

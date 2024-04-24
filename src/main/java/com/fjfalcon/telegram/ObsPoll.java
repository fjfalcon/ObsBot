package com.fjfalcon.telegram;

import com.fjfalcon.config.BotProperties;
import com.fjfalcon.inetmafia.client.InetMafiaService;
import com.fjfalcon.obs.ObsController;
import com.fjfalcon.youtube.YoutubeClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.List;

@Component
public class ObsPoll extends TelegramLongPollingBot {
    private final Logger logger = LoggerFactory.getLogger(ObsPoll.class);
    private final BotProperties botProperties;
    private final ObsController obsController;
    private final YoutubeClient youtubeClient;
    private final InetMafiaService inetMafiaService;

    public ObsPoll(BotProperties botProperties, ObsController obsController, YoutubeClient youtubeClient, InetMafiaService inetMafiaService) {
        super(botProperties.getPassword());
        this.botProperties = botProperties;
        this.obsController = obsController;
        this.youtubeClient = youtubeClient;
        this.inetMafiaService = inetMafiaService;
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            try {
                parseUpdate(update, update.getMessage().getText());
            } catch (TelegramApiException e) {
                throw new RuntimeException(e);
            }
        }

    }

    private void parseUpdate(Update update, String text) throws TelegramApiException {
        logger.info("{} sended {}", update.getMessage().getFrom().getUserName(), update.getMessage().getText());
        if (text.contains("/set")) {
            var msg = obsController.setUrl(text.substring(5));
            sendText(update, msg);
            if (!obsController.isStreamEnabled()) {
                sendText(update, "Запуск стрима на ютубе начат");
                youtubeClient.checkStreamStarted();
                sendText(update, "Запуск стрима на ютубе закончен");
            }
        } else if (text.contains("/get_live")) {
            sendText(update, youtubeClient.findLiveBroadcast());
        } else if (text.contains("/reset")) {
            var msg = obsController.reset();
            sendText(update, msg);
        } else if (text.contains("/start_obs")) {
            var msg = obsController.startStreaming();
            sendText(update, msg);
        } else if (text.contains("/stop_obs")) {
            youtubeClient.disableStream();
            var msg = obsController.stopStreaming();
            sendText(update, msg);
        } else if (text.contains("/list_streams")) {
            var msg = youtubeClient.listStream();
            sendText(update, msg);
        } else if (text.contains("/stop_stream")) {
            youtubeClient.stopStreaming();
            var msg = obsController.stopStreaming();
            sendText(update, msg);
        } else if (text.contains("/list_broadcasts")) {
            var msg = youtubeClient.listBroadcasts();
            sendText(update, msg);
        } else if (text.contains("/status")) {
            var msg = obsController.isStreamEnabled();
            sendText(update, String.valueOf(msg));
        } else if (text.contains("/game")) {
            var msg = inetMafiaService.isGameIsRunning(text.substring(6));
            sendText(update, String.format("Game %s is online: %s",text.substring(6), msg));
        }  else if (text.contains("/players")) {
        var msg = inetMafiaService.getPlayers(text.substring(9));
        sendText(update, msg);
    }

        else {
            sendText(update, "kek");
        }
    }

    private void sendText(Update update, String text) throws TelegramApiException {
        if (text.isBlank())
            return;
        SendMessage message = new SendMessage();
        message.setChatId(update.getMessage().getChatId());
        message.setText(text);
        sendApiMethod(message);
    }

    @Override
    public void onUpdatesReceived(List<Update> updates) {
        updates.forEach(this::onUpdateReceived);
    }

    @Override
    public String getBotUsername() {
        return botProperties.getName();
    }

    @Override
    public void onRegister() {
        super.onRegister();
    }
}

package com.fjfalcon.telegram;

import com.fjfalcon.config.BotProperties;
import com.fjfalcon.coordinator.StreamCoordinator;
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
import java.util.Map;
import java.util.function.BiConsumer;

import static com.fjfalcon.utils.LambdaUtils.wrapConsumer;

@Component
public class ObsPoll extends TelegramLongPollingBot {
    private final Logger logger = LoggerFactory.getLogger(ObsPoll.class);
    private final BotProperties botProperties;
    private final ObsController obsController;
    private final YoutubeClient youtubeClient;
    private final InetMafiaService inetMafiaService;
    private final StreamCoordinator streamCoordinator;

    private final Map<String, BiConsumer<Update, String>> commandActions = Map.ofEntries(
            Map.entry("/set", wrapConsumer(this::handleSet)),
            Map.entry("/getLive", wrapConsumer(this::handleGetLive)),
            Map.entry("/reset", wrapConsumer(this::handleReset)),
            Map.entry("/stop_stream", wrapConsumer(this::handleStopStream)),
            Map.entry("/list_broadcasts", wrapConsumer(this::handleListBroadcasts)),
            Map.entry("/status", wrapConsumer(this::handleStatus)),
            Map.entry("/players", wrapConsumer(this::handlePlayers)),
            Map.entry("/chat_id", wrapConsumer(this::handleChatId)),
            Map.entry("/follow", wrapConsumer(this::handleFollow)),
            Map.entry("/coordinator", wrapConsumer(this::handleCoordinator))
    );


    public ObsPoll(BotProperties botProperties, ObsController obsController, YoutubeClient youtubeClient, InetMafiaService inetMafiaService, StreamCoordinator streamCoordinator) {
        super(botProperties.getPassword());
        this.botProperties = botProperties;
        this.obsController = obsController;
        this.youtubeClient = youtubeClient;
        this.inetMafiaService = inetMafiaService;
        this.streamCoordinator = streamCoordinator;
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
        logger.info("{} {} @{} sent {}", update.getMessage().getFrom().getFirstName(), update.getMessage().getFrom().getLastName(),  update.getMessage().getFrom().getUserName(), update.getMessage().getText());
        var action = commandActions.get(text.split(" ")[0]);
        if (action == null) {
            sendText(update, "kek");
        } else action.accept(update, text);
    }

    private void handleCoordinator(Update update, String text) throws TelegramApiException {
        var msg = streamCoordinator.getKeys();
        sendText(update, msg);
        ;
    }

    private void handleFollow(Update update, String text) throws TelegramApiException {
        var msg = streamCoordinator.setFollowMode(text.substring(8));
        sendText(update, msg);
    }

    private void handleChatId(Update update, String text) throws TelegramApiException {
        sendText(update, update.getMessage().getChatId().toString());
    }

    private void handlePlayers(Update update, String text) throws TelegramApiException {
        var msg = inetMafiaService.getPlayers(text.substring(9));
        sendText(update, msg);
    }

    private void handleGame(Update update, String text) throws TelegramApiException {
        var msg = inetMafiaService.isGameIsRunning(text.substring(6));
        sendText(update, String.format("Game %s is online: %s", text.substring(6), msg));
    }

    private void handleStatus(Update update, String text) throws TelegramApiException {
        var msg = obsController.isStreamEnabled();
        sendText(update, String.valueOf(msg));
    }

    private void handleListBroadcasts(Update update, String text) throws TelegramApiException {
        var msg = youtubeClient.listBroadcasts();
        sendText(update, msg);
    }

    private void handleStopStream(Update update, String text) throws TelegramApiException {
        youtubeClient.stopStreaming();
        var msg = obsController.stopStreaming();
        sendText(update, msg);
    }

    private void handleListStreams(Update update, String text) throws TelegramApiException {
        var msg = youtubeClient.listStream();
        sendText(update, msg);
    }

    private void handleReset(Update update, String text) throws TelegramApiException {
        var msg = obsController.reset();
        sendText(update, msg);
    }

    private void handleGetLive(Update update, String text) throws TelegramApiException {
        sendText(update, youtubeClient.findLiveBroadcast());
    }

    private void handleSet(Update update, String text) throws TelegramApiException {
        var msg = obsController.setUrl(text.substring(5));
        sendText(update, msg);
        if (!obsController.isStreamEnabled()) {
            sendText(update, "Запуск стрима на ютубе начат");
            youtubeClient.checkStreamStarted();
            sendText(update, "Запуск стрима на ютубе закончен");
        }

        streamCoordinator.disableFollowMode();
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

    public void sendTextToMe(String text) {
        if (text.isBlank())
            return;
        SendMessage message = new SendMessage();
        message.setChatId(botProperties.getMyChatId());
        message.setText(text);
        try {
            sendApiMethod(message);
        } catch (TelegramApiException e) {
            logger.error("Failed to send message to master", e);
        }
    }
}

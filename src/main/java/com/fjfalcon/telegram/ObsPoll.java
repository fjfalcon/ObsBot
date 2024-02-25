package com.fjfalcon.telegram;

import com.fjfalcon.config.BotProperties;
import com.fjfalcon.obs.ObsController;
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

    public ObsPoll(BotProperties botProperties, ObsController obsController) {
        super(botProperties.getPassword());
        this.botProperties = botProperties;
        this.obsController = obsController;
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
            sendText(update,msg);
        } else if (text.contains("/reset")) {
            var msg = obsController.reset();
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

package de.noahalbers.plca.backend.chatmessenger;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.objects.Update;

import de.noahalbers.plca.backend.PLCA;
import de.noahalbers.plca.backend.logger.Logger;

public class TelegramBot extends TelegramLongPollingBot{
	
	// Reference to the main program
	private PLCA plca = PLCA.getInstance();
	
	// Logger
	private Logger log = new Logger("Telegram API");
	
	@Override
	public void onUpdateReceived(Update update) {
		// Checks that the sender is no bot himself
		if(update.getMessage().getFrom().getIsBot())
			return;
		
		// Logs the message
		this.log
		.debug("Received telegram-message")
		.critical(update.getMessage().getText());
	}

	@Override
	public String getBotUsername() {
		return this.plca.getConfig().getUnsafe("botname");
	}

	@Override
	public String getBotToken() {
		return this.plca.getConfig().getUnsafe("token");
	}

}

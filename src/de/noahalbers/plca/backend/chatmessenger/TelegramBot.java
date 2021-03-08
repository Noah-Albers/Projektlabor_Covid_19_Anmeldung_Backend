package de.noahalbers.plca.backend.chatmessenger;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.objects.Update;

import de.noahalbers.plca.backend.Config;

public class TelegramBot extends TelegramLongPollingBot{
	
	@Override
	public void onUpdateReceived(Update update) {
		System.out.println("Update: "+update.getMessage().getText());
	}

	@Override
	public String getBotUsername() {
		return Config.getInstance().get("botname");
	}

	@Override
	public String getBotToken() {
		return Config.getInstance().get("token");
	}

}

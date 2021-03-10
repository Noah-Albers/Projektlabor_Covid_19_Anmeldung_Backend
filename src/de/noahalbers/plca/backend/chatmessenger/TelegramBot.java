package de.noahalbers.plca.backend.chatmessenger;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.objects.Update;

import de.noahalbers.plca.backend.PLCA;

public class TelegramBot extends TelegramLongPollingBot{
	
	// Reference to the main program
	private PLCA plca = PLCA.getInstance();
	
	@Override
	public void onUpdateReceived(Update update) {
		System.out.println("Update: "+update.getMessage().getText());
	}

	@Override
	public String getBotUsername() {
		return this.plca.getConfig().getString("botname");
	}

	@Override
	public String getBotToken() {
		return this.plca.getConfig().getString("token");
	}

}

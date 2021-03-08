package de.noahalbers.plca.backend;

import java.io.IOException;

import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import de.noahalbers.plca.backend.chatmessenger.TelegramBot;

public class Main {
	
	public static void main(String[] args) throws Exception {
		new Main();
	}
	
	public Main() {
		// Loads the config
		try {
			// TODO: Handle exception
			Config.getInstance()
				.register("token", null)
				.register("botname", null)
				.loadConfig();
		} catch (IOException e1) {
			e1.printStackTrace();
			return;
		}
		
		// TODO: Handle exceptions
		try {
			// Starts the telegram api
			TelegramBotsApi api = new TelegramBotsApi(DefaultBotSession.class);
			api.registerBot(new TelegramBot());
		} catch (TelegramApiException e) {
			e.printStackTrace();
			return;
		}
		
		System.out.println("Main stuff or smth");
	}
}

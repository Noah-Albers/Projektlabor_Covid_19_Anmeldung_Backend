package de.noahalbers.plca.backend;

import java.io.IOException;
import java.sql.SQLException;

import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import de.noahalbers.plca.backend.chatmessenger.TelegramBot;
import de.noahalbers.plca.backend.database.PLCADatabase;

public class Main {
	
	public static void main(String[] args) throws Exception {
		new Main();
	}
	
	public Main() {
		try {
			// TODO: Handle exception
			// Loads the config
			Config.getInstance()
				.register("token", null)
				.register("botname", null)
				.register("db_host", "localhost")
				.register("db_port", "3306")
				.register("db_user", "root")
				.register("db_password", "")
				.register("db_databasename", "test")
				.loadConfig();
		} catch (IOException e1) {
			e1.printStackTrace();
			return;
		}

		// Gets the database handler
		new PLCADatabase();
		
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

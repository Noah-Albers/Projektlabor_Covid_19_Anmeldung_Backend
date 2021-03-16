package de.noahalbers.plca.backend;

import java.io.IOException;
import java.util.Optional;

import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import de.noahalbers.plca.backend.chatmessenger.TelegramBot;
import de.noahalbers.plca.backend.database.PLCADatabase;
import de.noahalbers.plca.backend.server.PLCAServer;

public class PLCA {

	// Singleton instance
	private static PLCA SINGLETON_INSTANCE;
	
	// Telegram bot
	private TelegramBot messenger;
	
	// Connection handler for the database
	private PLCADatabase database;
	
	// Server handler for connection (Covid-login and admins)
	private PLCAServer server;
	
	// Config for the program
	private Config config = new Config()
			.register("token", null)
			.register("botname", null)
			.register("db_host", "localhost")
			.register("db_port", "3306")
			.register("db_user", "root")
			.register("db_password", "")
			.register("db_databasename", "test")
			.register("connection_timeout", "5000")
			.register("applogin_pubK", "")
			.register("port", "1337");
	
	private PLCA() {
		SINGLETON_INSTANCE = this;
	}

	/**
	 * Inits and starts the program. Should only be called once.
	 */
	public void init() {
		
		// Ensures that the current runtime support rsa and aes
		Optional<String> optError = new EncryptionManager().init();
		
		// Checks if one of them isn't supported
		if(new EncryptionManager().init().isPresent()) {
			System.out.println(optError.get()+" is not supported.");
			return;
		}
		
		try {
			// TODO: Handle exception
			// Loads the config	
			this.config.loadConfig();
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}
		
		// Creates the database
		this.database = new PLCADatabase();
		
		// TODO: Handle exceptions
		try {
			// Starts the telegram api
			TelegramBotsApi api = new TelegramBotsApi(DefaultBotSession.class);
			api.registerBot(this.messenger = new TelegramBot());
		} catch (TelegramApiException e) {
			e.printStackTrace();
			return;
		}
		
		// TODO: handle exception
		try {
			// Start the server
			this.server = new PLCAServer();
			this.server.start();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public TelegramBot getMessenger() {
		return this.messenger;
	}

	public PLCADatabase getDatabase() {
		return this.database;
	}

	public Config getConfig() {
		return this.config;
	}
	
	public PLCAServer getServer() {
		return this.server;
	}
	
	public static PLCA getInstance() {
		if(SINGLETON_INSTANCE == null)
			SINGLETON_INSTANCE = new PLCA();
		return SINGLETON_INSTANCE;
	}
	
}

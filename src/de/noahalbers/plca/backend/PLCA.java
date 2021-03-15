package de.noahalbers.plca.backend;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.Optional;

import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import de.noahalbers.plca.backend.chatmessenger.TelegramBot;
import de.noahalbers.plca.backend.database.PLCADatabase;
import de.noahalbers.plca.backend.socket.PLCAConnection;

public class PLCA {

	// Singleton instance
	private static PLCA SINGLETON_INSTANCE;
	
	// Telegram bot
	private TelegramBot messenger;
	
	// Connection handler for the database
	private PLCADatabase database;
	
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
			.register("applogin_pubK", "");
	
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
		} catch (IOException e1) {
			e1.printStackTrace();
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
		
		// TODO: Handle exceptions
		try {
			// TODO: make better
			@SuppressWarnings("resource")
			ServerSocket ss = new ServerSocket(1337);
			
			while(true) {
				new PLCAConnection(ss.accept(), System.out::println).start();
			}
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
	
	public static PLCA getInstance() {
		if(SINGLETON_INSTANCE == null)
			SINGLETON_INSTANCE = new PLCA();
		return SINGLETON_INSTANCE;
	}
	
}

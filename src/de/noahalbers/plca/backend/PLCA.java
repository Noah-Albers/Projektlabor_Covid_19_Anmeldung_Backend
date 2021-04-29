package de.noahalbers.plca.backend;

import java.io.IOException;
import java.math.BigInteger;
import java.security.spec.RSAPublicKeySpec;
import java.util.Optional;
import java.util.TimeZone;

import de.noahalbers.plca.backend.backup.BackgroundTask;
import de.noahalbers.plca.backend.config.Config;
import de.noahalbers.plca.backend.config.ConfigLoadException;
import de.noahalbers.plca.backend.config.loaders.IntegerValue;
import de.noahalbers.plca.backend.config.loaders.LongValue;
import de.noahalbers.plca.backend.config.loaders.RSAPublicKeyValue;
import de.noahalbers.plca.backend.config.loaders.StringValue;
import de.noahalbers.plca.backend.database.PLCADatabase;
import de.noahalbers.plca.backend.email.EmailService;
import de.noahalbers.plca.backend.logger.Logger;
import de.noahalbers.plca.backend.server.PLCAServer;

public class PLCA {

	// Singleton instance
	private static PLCA SINGLETON_INSTANCE;
	
	// Connection handler for the database
	private PLCADatabase database;
	
	// Server handler for connection (Covid-login and admins)
	private PLCAServer server;
	
	// Logger
	private Logger log = new Logger("PLCA-Main");
	
	// Email service
	private EmailService emailManager;
	
	// Config for the program
	private Config config = new Config()
			.register("db_host", new StringValue("localhost"),"Domain/Ip of the database host. Will usually be something like localhost")
			.register("db_port", new IntegerValue(3306),"Port on which the database-server is running")
			.register("db_user", new StringValue("root"),"Username that shall be used to access the database")
			.register("db_password", new StringValue(""),"Password that shall be used to access the database")
			.register("db_databasename", new StringValue("test"),"The name of the database")
			.register("connection_timeout", new LongValue(5000l),"How long to wait until a connection gets closed. Time in ms")
			.register("applogin_pubK", new RSAPublicKeyValue(new RSAPublicKeySpec(new BigInteger("0"),new BigInteger("0"))),"The rsa-public-key in json-format that is used by the login-application. Is required to authenticate the login-app")
			.register("port", new IntegerValue(1337),"On which port the server that is waiting for connections is running")
			.register("backup_delay", new LongValue(1000 * 60l),"How long to wait between backups. Time in ms")
			.register("email_host", new StringValue(""),"Domain/Ip of the remote email server")
			.register("email_mail", new StringValue(""),"Email-address that is used to send the backup-mail")
			.register("email_port", new IntegerValue(587),"On which port the remote email server is running")
			.register("email_password", new StringValue(""),"Password that is used to access the email-account")
			.register("backup_email_subject", new StringValue("Database-Backup"),"The subject that will be send on the email")
			.register("backup_email_filename", new StringValue("Backup.sql.enc"),"The filename of the encrypted database file that will be send using the email")
			.register("backup_email_encryption_key", new StringValue(""),"The raw aes-key that shall be used to encrypt the backup file before sending it")
			.register("backup_autologout", new LongValue(1000l*60*60),"How long to wait between the auto-logout processes. Time in ms")
			.register("autodelete_time", new LongValue(1000*60*60*24*7*4l),"How long a user needs to be inactive until his account can be deleted. Time in ms")
			.register("autologout_after_time", new IntegerValue(24),"How long a user has to be logged in to get logged out automatically.")
			.register("admin_auth_expire", new LongValue(1000l * 60 * 60 * 24),"How many millis it takes until a generated auth code expires.")
			.register("admin_auth_email_subject", new StringValue("PL-Admin-interface Authcode"),"The Email-subject that will be send when sending a auth code to an email.")
			.register("admin_auth_email_html", new StringValue("<h1>Ihr Authentifizierungscode lautet: <span style=\"font-size:150%;font-family:monospace;border-radius:5px;background:gray;padding:2px 5px;color:white;\">%code%</span></h1>"),"The HTML-Content of the email the embeds the authcode. The text %code% will be replaced with the actual code.")
			;
	
	private PLCA() {
		SINGLETON_INSTANCE = this;
	}

	/**
	 * Inits and starts the program. Should only be called once.
	 */
	public void init() {
		
		this.log.info("Will be using "+TimeZone.getDefault().getID()+" as the timezone.");
		
		this.database = new PLCADatabase();
		
		// Ensures that the current runtime support rsa and aes
		Optional<String> optError = new EncryptionManager().init();
		
		this.log.info("Checking Encryption services...");
		
		// Checks if one of them isn't supported
		if(optError.isPresent()) {
			this.log
			.error("Failed to start the encryption system, "+optError.get()+" is not supported.");
			this.shutdown();
			return;
		}
		
		this.log.info("Loading config...");
		
		try {
			// Loads the config	
			this.config.loadConfig();
		} catch (IOException e) {
			this.log
			.error("Failed to load config")
			.error(e);
			this.shutdown();
			return;
		}catch(ConfigLoadException e) {
			this.log.error("Failed to load config (Attribute is invalid)").error("Attribute="+e.getAttributeName());
			this.shutdown();
			return;
		}
		
		this.log.info("Starting email service");
		
		// Starts the email manager
		this.emailManager = new EmailService(
			this.config.getUnsafe("email_mail"),
			this.config.getUnsafe("email_host"),
			this.config.getUnsafe("email_password"),
			this.config.getUnsafe("email_port")
		);
		
		this.log.info("Starting background tasks");
		
		// Starts the backup task
		try {
			BackgroundTask backup = new BackgroundTask();
			backup.start();
		} catch (Exception e) {
			this.log
			.error("Failed to start background task")
			.error(e);
			this.shutdown();
			return;
		}
		
		this.log.info("Starting request server");
		
		try {
			// Start the server
			this.server = new PLCAServer();
			this.server.start();
		} catch (IOException e) {
			this.log
			.error("Failed to start PLCA-Server")
			.error(e);
			this.shutdown();
		}
	}
	
	/**
	 * Shuts the program down
	 */
	private void shutdown() {
		System.exit(-1);
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
	
	public EmailService getEmailService() {
		return this.emailManager;
	}
	
	public static PLCA getInstance() {
		if(SINGLETON_INSTANCE == null)
			SINGLETON_INSTANCE = new PLCA();
		return SINGLETON_INSTANCE;
	}
}

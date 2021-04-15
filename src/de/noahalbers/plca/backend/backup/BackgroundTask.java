package de.noahalbers.plca.backend.backup;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Optional;

import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import de.noahalbers.plca.backend.EncryptionManager;
import de.noahalbers.plca.backend.PLCA;
import de.noahalbers.plca.backend.config.Config;
import de.noahalbers.plca.backend.database.PLCADatabase;
import de.noahalbers.plca.backend.logger.Logger;
import de.noahalbers.plca.backend.util.PresetTimer;

public class BackgroundTask extends Thread {

	// Reference to the main app
	private PLCA plca = PLCA.getInstance();

	// Reference to the logger
	private Logger log = new Logger("Background Task");

	// Reference to the config
	private Config config = this.plca.getConfig();

	// Reference to the database
	private PLCADatabase database = this.plca.getDatabase();

	// Timers for the backup and autologout
	private PresetTimer backupTimer = new PresetTimer(this.plca.getConfig().getUnsafe("backup_delay"));
	private PresetTimer autologoutTimer = new PresetTimer(this.plca.getConfig().getUnsafe("backup_autologout"));
	
	// Encryption manager to encrypt the email backup
	private EncryptionManager encryptionManager = new EncryptionManager();

	// Values
	private SecretKeySpec aesKey;
	private IvParameterSpec aesIV;

	public BackgroundTask() throws Exception {
		// Starts the encryption manager
		Optional<String> optErr = this.encryptionManager.init();

		// Checks if the encryption system failed to start
		if (optErr.isPresent())
			throw new Exception("Failed to start the background tasks: Could not start encryption system: " + optErr.get());

		// Gets the raw aes key
		byte[] rawKey = ((String)this.config.get("backup_email_encryption_key")).getBytes(StandardCharsets.UTF_8);

		// Generates the aes-values from the raw key
		this.aesIV = new IvParameterSpec(this.encryptionManager.hashMD5(rawKey));
		this.aesKey = new SecretKeySpec(this.encryptionManager.hashSHA256(rawKey), "AES");
	}

	@Override
	public void run() {
		// Logs the starting of the process
		this.log.info("Started background task");

		// Runs as long as not aborted
		while (!Thread.currentThread().isInterrupted()) {

			try {
				// Delay
				Thread.sleep(1000);
			} catch (InterruptedException e) {}
			
			
			// Checks if the autologout should be executed
			if(this.autologoutTimer.hasReached()) {
				this.handleAutologout();
				this.autologoutTimer.reset();
				continue;
			}
			
			// Checks if the backup timer is finished
			if(this.backupTimer.hasReached()) {
				this.handleBackup();
				this.backupTimer.reset();
				continue;
			}
		}
	}

	
	/**
	 * Executes when a backup should be taken
	 */
	private void handleBackup() {
		try{
			// Logs the starting of the backup
			this.log.info("Starting backup task, starting autodelete of old accounts...");

			// Starts the connection
			try(Connection con = this.database.startConnection())
			{
				// Deletes old accounts
				this.database.doAutoDeleteAccounts(con);
			}
			
			this.log.debug("Removed old accounts, requesting database backup");
			
			// Gets the backup from the database
			byte[] backup = this.database.requestDatabaseBackup().getBytes(StandardCharsets.UTF_8);

			// Encrypts the backup
			Optional<byte[]> encryptedBackup = this.encryptionManager.encryptAES(backup, this.aesKey, this.aesIV);

			// Checks if the encryption failed
			if (!encryptedBackup.isPresent())
				throw new Exception("Failed to encrypt the backup");

			this.log.debug("Sending mail");
			
			// Uploads the backup to the server
			this.plca.getEmailService().uploadFile(
				this.config.getUnsafe("backup_email_filename"),
				encryptedBackup.get(),
				this.config.getUnsafe("backup_email_subject")
			);
		} catch (Exception e1) {
			this.log
			.error("Error while taking backup")
			.critical(e1);
		}
	}
	
	/**
	 * Executes when an autologout for old logins should be executed
	 */
	private void handleAutologout() {
		try(Connection con = this.database.startConnection()){
			this.log.info("Starting autologout");
			// Automatically logs out users
			this.database.doAutologoutUsers(con);
		} catch (SQLException e) {
			this.log
			.error("Error while logging old users out")
			.critical(e);
		}
	}
}

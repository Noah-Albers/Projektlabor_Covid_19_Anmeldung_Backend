package de.noahalbers.plca.backend.backup;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Optional;
import java.util.Properties;

import javax.activation.DataHandler;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.util.ByteArrayDataSource;

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
		byte[] rawKey = ((String)this.config.get("email_encryption_key")).getBytes(StandardCharsets.UTF_8);

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
			
			// "Uploads" the backup to the server
			this.sendEmail(encryptedBackup.get());
		} catch (Exception e1) {
			this.log.error("Error while taking backup").critical(e1);
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
			this.log.error("Error while logging old users out").critical(e);
		}
	}
	
	
	/**
	 * Sends an email with the given sqlBackup-string to the configured provider to
	 * store as a backup
	 * 
	 * @param encryptedBackup
	 *            the encrypted bytes of the sql-backup
	 * @throws MessagingException
	 *             if anything went wrong with sending the email
	 */
	private void sendEmail(byte[] encryptedBackup) throws MessagingException {
		// Gets the email
		String email = this.config.getUnsafe("email_mail");
		// Gets the host
		String host = this.config.getUnsafe("email_host");
		// Gets the password
		String passwd = this.config.getUnsafe("email_password");
		// Gets the filename
		String filename = this.config.getUnsafe("email_filename");
		// Gets the email subject
		String subject = this.config.getUnsafe("email_subject");

		Properties prop = new Properties();
		prop.put("mail.smtp.auth", true);
		prop.put("mail.smtp.starttls.enable", "true");
		prop.put("mail.smtp.host", host);
		prop.put("mail.smtp.port", this.config.get("email_port"));
		prop.put("mail.smtp.ssl.trust", host);

		// Prepares the session to connect to the provider
		Session session = Session.getInstance(prop, new Authenticator() {
			@Override
			protected PasswordAuthentication getPasswordAuthentication() {
				return new PasswordAuthentication(email, passwd);
			}
		});

		// Body-part that attaches the file
		MimeBodyPart mbp = new MimeBodyPart() {
			{
				this.setDataHandler(
						new DataHandler(new ByteArrayDataSource(encryptedBackup, "application/octet-stream")));
				this.setFileName(filename);
			}
		};

		// Creates the email that will be send
		Message message = new MimeMessage(session) {
			{
				this.setFrom(new InternetAddress(email));
				this.setRecipients(Message.RecipientType.TO, InternetAddress.parse(email));
				this.setSubject(subject);
				this.setContent(new MimeMultipart(mbp));
			}
		};

		// Sends the email (Message)
		Transport.send(message);
	}

}

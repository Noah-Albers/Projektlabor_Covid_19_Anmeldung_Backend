package de.noahalbers.plca.backend.email;

import java.util.Properties;

import javax.activation.DataHandler;
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

public class EmailService {

	// Email config
	private Properties emailConfig;
	
	public EmailService(String email,String host,String password,int smtpPort) {
		this.emailConfig = new Properties() {
			private static final long serialVersionUID = 1L;
			{
				put("mail.smtp.auth", true);
				put("mail.smtp.starttls.enable", "true");
				put("mail.smtp.host", host);
				put("mail.smtp.port", smtpPort);
				put("mail.smtp.ssl.trust", host);
				put("email",email);
				put("password",password);
			}
		};
	}
	
	/**
	 * Sends the given html-content to the given email address
	 * @param emailAddress the address to send the email to
	 * @param subject the subject of the email
	 * @param htmlContent the raw html that will be displayed
	 * @throws MessagingException if anything went wrong with sending the email
	 */
	public void sendHTMLEmail(String emailAddress,String subject,String htmlContent) throws MessagingException {		
		// Gets the mail address
		String email = this.emailConfig.getProperty("email");
		String password = this.emailConfig.getProperty("password");
		
		// Prepares the session to connect to the provider
		Session session = Session.getInstance(this.emailConfig, new Authenticator() {
			@Override
			protected PasswordAuthentication getPasswordAuthentication() {
				return new PasswordAuthentication(email, password);
			}
		});

		// Creates the email that will be send
		Message message = new MimeMessage(session) {
			{
				setFrom(new InternetAddress(email));
				setRecipients(Message.RecipientType.TO, InternetAddress.parse(emailAddress));
				setSubject(subject);
				setContent(htmlContent,"text/html");
			}
		};

		// Sends the email / Uploads the file
		Transport.send(message);
	}
	
	/**
	 * Sends an email with the provided file to itself. Therefore storing the file online at the email-provider
	 * @param filename the filename (Plus extension eg. .txt, .sql, .html)
	 * @param fileData the raw bytes that shall be stored inside the file
	 * @param emailSubject the subject that shall be send as the email title
	 * @throws MessagingException if anything went wrong
	 */
	public void uploadFile(String filename,byte[] fileData,String emailSubject) throws MessagingException {
		
		// Gets the mail address
		String email = this.emailConfig.getProperty("email");
		String password = this.emailConfig.getProperty("password");
		
		// Prepares the session to connect to the provider
		Session session = Session.getInstance(this.emailConfig, new Authenticator() {
			@Override
			protected PasswordAuthentication getPasswordAuthentication() {
				return new PasswordAuthentication(email, password);
			}
		});

		// Body-part that attaches the file
		MimeBodyPart mbp = new MimeBodyPart() {
			{
				setDataHandler(
						new DataHandler(new ByteArrayDataSource(fileData, "application/octet-stream")));
				setFileName(filename);
			}
		};

		// Creates the email that will be send
		Message message = new MimeMessage(session) {
			{
				setFrom(new InternetAddress(email));
				setRecipients(Message.RecipientType.TO, InternetAddress.parse(email));
				setSubject(emailSubject);
				setContent(new MimeMultipart(mbp));
			}
		};

		// Sends the email / Uploads the file
		Transport.send(message);
	}
	
}

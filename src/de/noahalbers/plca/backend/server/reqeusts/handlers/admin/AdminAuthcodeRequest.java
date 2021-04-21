package de.noahalbers.plca.backend.server.reqeusts.handlers.admin;

import java.io.IOException;
import java.security.SecureRandom;
import java.sql.SQLException;
import java.sql.Timestamp;

import javax.mail.MessagingException;

import de.noahalbers.plca.backend.PLCA;
import de.noahalbers.plca.backend.config.Config;
import de.noahalbers.plca.backend.database.entitys.AdminEntity;
import de.noahalbers.plca.backend.database.exceptions.EntitySaveException;
import de.noahalbers.plca.backend.server.reqeusts.Request;
import de.noahalbers.plca.backend.server.reqeusts.RequestHandler;
import de.noahalbers.plca.backend.server.reqeusts.checks.PermissionCheck;
import de.noahalbers.plca.backend.server.reqeusts.checks.PermissionChecks;

public class AdminAuthcodeRequest extends RequestHandler {
	
	/**
	 * Responses:
	 * 	Errors:
	 * 		database: Backend failed to establish a valid database connection
	 * 		unknown: an unknown error occurre. This should not happen. Please try again.	
	 * 		email: an error occurred while sending the email. Is the email not existing or is it a server-side problem
	 * 
	 * 	Success: Empty (Email with auth code has been send successfully)
	 */
	
	// Random generator
	private static final SecureRandom RDM_GENERATOR = new SecureRandom();
	
	// Email details
	private static final String EMAIL_CONT,EMAIL_SUBJECT;
	
	// How long until an auth code expires
	private static final long AUTH_EXPIRE_TIME;

	static {
		// Gets the config
		Config cfg = PLCA.getInstance().getConfig();
		
		// Gets the constant values
		AUTH_EXPIRE_TIME = cfg.getUnsafe("admin_auth_expire");
		EMAIL_CONT = cfg.getUnsafe("admin_auth_email_html");
		EMAIL_SUBJECT = cfg.getUnsafe("admin_auth_email_subject");
	}
	
	@Override
	public void execute(Request request) throws IOException {
		// Gets the admin
		AdminEntity adm = request.getAdmin().get();
		// Generates a random next code
		adm.authCode = RDM_GENERATOR.nextLong();
		// Updates the expire date
		adm.authCodeExpire = new Timestamp(System.currentTimeMillis()+AUTH_EXPIRE_TIME);
		
		try {
			// Updates the admin on the database
			this.database.updateAdmin(request.startDatabaseConnection(), adm);
		} catch (SQLException e) {
			this.sendErrorDatabase(request, e);
			return;
		} catch (EntitySaveException e) {
			this.sendErrorUnknownException(request, e);
			return;
		}
		
		// Sends the email
		try {
			PLCA.getInstance().getEmailService().sendHTMLEmail(
				adm.email,
				EMAIL_SUBJECT,
				EMAIL_CONT.replace("%code%", adm.authCode.toString())
			);
		} catch (MessagingException e) {
			request.logger.debug("Failed to send email").critical(e);
			request.sendError("email");
			return;
		}
		
		// Sends the successful reset of the code
		request.sendResponse(null);
	}

	@Override
	public PermissionCheck[] getPermissionChecks() {
		return of(PermissionChecks.PERM_ADMIN);
	}

}

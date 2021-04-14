package de.noahalbers.plca.backend.server.reqeusts.checks;

import java.sql.Timestamp;

import de.noahalbers.plca.backend.database.entitys.AdminEntity;

public final class RequestChecks {
	
	private RequestChecks() {};
	
	/**
	 * Checks that the requesting admin has send a valid id
	 * 
	 * Reqeust:
	 * 	auth: The authcode of the admin (Long)
	 * 
	 * Response:
	 * 	auth.missing: The authcode has not been provided
	 * 	auth.invalid: The given authcode is not valid
	 * 	auth.expired: The authcode is valid but the authcode is expired
	 */
	public static RequestCheck ADMIN_AUTH_CODE = req->{
		
		// Gets the auth code
		Long authCode = req.getFromMessage("auth", Long.class);
		if(authCode==null) {
			req.logger.debug("Auth code is missing.");
			req.sendError("auth.missing");
			return false;
		}
		
		// Gets the admin
		AdminEntity adm = req.getAdmin().get();
		
		// Checks if the auth code is invalid
		if(adm.authCode == null || adm.authCodeExpire == null || authCode != adm.authCode) {
			req.logger.debug("Provided auth code is invalid").critical("AuthCode="+adm.authCode);
			req.sendError("auth.invalid");
			return false;
		}
		
		// Current date
		Timestamp now = new Timestamp(System.currentTimeMillis());
		
		// Checks if the auth is expired
		if(adm.authCodeExpire.after(now)) {
			req.logger.debug("Auth code is expired").critical("AuthCode="+adm.authCode+"; Expired="+adm.authCodeExpire);
			req.sendError("auth.expired");
			return false;
		}
		
		return true;
	};
	
	/**
	 * Checks that the requesting admin has an account that is not frozen
	 * 
	 * Response:
	 * 	auth.frozen: The account that is request is frozen. The request can only be performed by an unfrozen account
	 */
	public static RequestCheck ADMIN_NOT_FROZEN = req->{
		// Checks if the the account is frozen
		if(req.getAdmin().get().isFrozen) {
			req.logger.debug("Account is frozen");
			req.sendError("auth.frozen");
			return false;
		}
		return true;
	};
}

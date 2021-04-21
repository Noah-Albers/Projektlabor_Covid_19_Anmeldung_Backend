package de.noahalbers.plca.backend.server.reqeusts.checks;

import java.sql.Timestamp;
import java.util.AbstractMap;
import java.util.Map.Entry;

import org.json.JSONObject;

import de.noahalbers.plca.backend.database.entitys.AdminEntity;
import de.noahalbers.plca.backend.server.reqeusts.Permissions;
import de.noahalbers.plca.backend.server.reqeusts.Request;

public final class PermissionChecks {
	
	private PermissionChecks() {};

	/**
	 * Checks if the requesting user has the permissions to access the required resource
	 * 
	 * Response:
	 * 	auth: The user does not have permissions to access the resource
	 */
	public static PermissionCheck PERM_ADMIN = req->performPermCheck(req, Permissions.ADMIN);
	public static PermissionCheck PERM_RESET_ADMIN = req->performPermCheck(req, Permissions.RESET_ADMIN);
	public static PermissionCheck PERM_DEFAULT_LOGIN = req->performPermCheck(req, Permissions.DEFAULT_LOGIN);

	// Helping function to perm permission checks
	public static Entry<String,JSONObject> performPermCheck(Request req, int permission){
		// Gets the requesters permissions
		int reqPerm = req.getAdmin().isPresent() ? req.getAdmin().get().permissions : Permissions.DEFAULT_LOGIN;
		
		// Performs the permission check
		return (reqPerm & permission) == 0 ? of("auth") : null;
	}
	
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
	public static PermissionCheck CHECK_ADMIN_AUTH_CODE = req->{
		
		// Gets the auth code
		Number authCode = req.getFromAuth("code", Number.class);
		if(authCode==null) {
			req.logger.debug("Auth code is missing.");
			return of("auth.missing");
		}
		
		// Gets the admin
		AdminEntity adm = req.getAdmin().get();
		
		// Checks if the auth code is invalid
		if(adm.authCode == null || adm.authCodeExpire == null || authCode.longValue() != adm.authCode) {
			req.logger.debug("Provided auth code is invalid").critical("AuthCode="+adm.authCode);
			return of("auth.invalid");
		}
		
		// Current date
		Timestamp now = new Timestamp(System.currentTimeMillis());
		
		// Checks if the auth is expired
		if(now.after(adm.authCodeExpire)) {
			req.logger.debug("Auth code is expired").critical("AuthCode="+adm.authCode+"; Expired="+adm.authCodeExpire);
			return of("auth.expired");
		}
		
		return null;
	};
	
	/**
	 * Checks that the requesting admin has an account that is not frozen
	 * 
	 * Response:
	 * 	auth.frozen: The account that is request is frozen. The request can only be performed by an unfrozen account
	 */
	public static PermissionCheck CHECK_ADMIN_NOT_FROZEN = req->{
		// Checks if the the account is frozen
		if(req.getAdmin().get().isFrozen) {
			req.logger.debug("Account is frozen");
			return of("auth.frozen");
		}
		return null;
	};
	
	/**
	 * Returns true if no admin is requesting otherwise performs an not frozen check
	 */
	public static PermissionCheck IF_ADMIN_CHECK_NOT_FROZEN = req->{
		return req.getAdmin().isPresent() ?
				CHECK_ADMIN_NOT_FROZEN.checkRequest(req) :
				null;
	};

	/**
	 * Returns true if no admin is requesting otherwise performs an auth check
	 */
	public static PermissionCheck IF_ADMIN_CHECK_AUTH_CODE = req->{
		return req.getAdmin().isPresent() ?	
			CHECK_ADMIN_AUTH_CODE.checkRequest(req) : 
			null;
	};
	
	
	// Methods to easily generate a entry with the required values
	private static Entry<String,JSONObject> of(String key) {
		return of(key,null);
	}
	private static Entry<String,JSONObject> of(String key,JSONObject data) {
		return new AbstractMap.SimpleEntry<String, JSONObject>(key,data == null ? new JSONObject() : data);		
	}
}

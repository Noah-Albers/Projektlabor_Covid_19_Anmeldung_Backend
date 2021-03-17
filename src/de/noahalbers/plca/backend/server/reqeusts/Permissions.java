package de.noahalbers.plca.backend.server.reqeusts;

public class Permissions {
	
	public static final int
			DEFAULT_LOGIN = 0b1,		// The default login (Covid-login). It is not set, but expect that
										// normal users are able to access this permission
			ADMIN = 0b10,				// This user is a verified admin that is allowed to modify and access critical
										// user info
			RESET_ADMIN = 0b100;		// This user is verified to reset an admins key and account

}

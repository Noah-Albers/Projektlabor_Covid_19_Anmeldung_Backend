package de.noahalbers.plca.backend.server.reqeusts.handlers.admin;

import java.io.IOException;
import java.sql.SQLException;

import de.noahalbers.plca.backend.database.entitys.SimpleUserEntity;
import de.noahalbers.plca.backend.database.entitys.UserEntity;
import de.noahalbers.plca.backend.database.exceptions.EntityLoadException;
import de.noahalbers.plca.backend.database.exceptions.EntitySaveException;
import de.noahalbers.plca.backend.server.reqeusts.Request;
import de.noahalbers.plca.backend.server.reqeusts.RequestHandler;
import de.noahalbers.plca.backend.server.reqeusts.checks.PermissionCheck;
import de.noahalbers.plca.backend.server.reqeusts.checks.PermissionChecks;

public class AdminEditUserReqeust extends RequestHandler{

	/**
	 * Responses:
	 * 	Errors:
	 * 		user: Failed to load the user
	 * 		database: Backend failed to establish a valid database connection
	 * 		unknown: An unkown error occurred
	 * 	
	 * 	Success:
	 * 		Empty
	 * 	
	 * Request:
	 * 		Loadable {@link UserEntity} Required={@link #REQUIRED} and Optional={@link #OPIONAL}
	 */
	
	// Required and optional values that shall be passed by an admin
	private static final String[] REQUIRED = {
		SimpleUserEntity.ID,
		UserEntity.POSTAL_CODE,
		UserEntity.LOCATION,
		UserEntity.STREET,
		UserEntity.HOUSE_NUMBER,
		UserEntity.AUTODELETE,
		UserEntity.REGISTER_DATE,
		SimpleUserEntity.FIRSTNAME,
		SimpleUserEntity.LASTNAME
	};
	
	private static final String[] OPIONAL = {		
		UserEntity.TELEPHONE,
		UserEntity.EMAIL,
		UserEntity.RFID,
	};
	
	@Override
	public PermissionCheck[] getPermissionChecks() {
		return of(
			PermissionChecks.PERM_ADMIN,
			PermissionChecks.CHECK_ADMIN_AUTH_CODE,
			PermissionChecks.CHECK_ADMIN_NOT_FROZEN
		);
	}

	@Override
	public void execute(Request request) throws IOException {
		// Will hold the user
		UserEntity user = new UserEntity();
		try {
			// Loads the user
			user.load(request.getMessage(), REQUIRED, OPIONAL);
		} catch (EntityLoadException e) {
			request.logger.debug("Failed to load user from passed values.").critical(e);
			request.sendError("user");
			return;
		}
		
		try {
			// Updates the user on the database
			this.database.updateUser(request.startDatabaseConnection(), user);
			
			// Sends back the success
			request.sendResponse(null);
		} catch (SQLException e) {
			this.sendErrorDatabase(request, e);
		} catch (EntitySaveException e) {
			this.sendErrorUnknownException(request, e);
		}
	}

}

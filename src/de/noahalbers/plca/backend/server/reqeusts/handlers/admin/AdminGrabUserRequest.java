package de.noahalbers.plca.backend.server.reqeusts.handlers.admin;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Optional;

import org.json.JSONObject;

import de.noahalbers.plca.backend.database.entitys.UserEntity;
import de.noahalbers.plca.backend.database.exceptions.EntitySaveException;
import de.noahalbers.plca.backend.server.reqeusts.Request;
import de.noahalbers.plca.backend.server.reqeusts.RequestHandler;
import de.noahalbers.plca.backend.server.reqeusts.checks.PermissionCheck;
import de.noahalbers.plca.backend.server.reqeusts.checks.PermissionChecks;

public class AdminGrabUserRequest extends RequestHandler{

	/**
	 * Responses:
	 * 	Errors:
	 * 		user: No user given
	 *		not_found: The given user could not be found
	 *		
	 * 		database: Backend failed to establish a valid database connection
	 * 		unknown: An unkown error occurred
	 * 	
	 * 	Success:
 * 			Loadable {@link UserEntity} with {@link UserEntity.DB_ENTRY_LIST}
	 * 	
	 * Request:
	 * 	user: the user-id of the user
	 */
	
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
		// Gets the user id
		Integer userId = request.getFromMessage("user", Integer.class);
		if(userId==null) {
			this.sendErrorMissingField(request, "user");
			return;
		}
		
		try {
			// Gets the user
			Optional<UserEntity> optUser = this.database.getUser(request.startDatabaseConnection(), userId);
			
			// Checks if the user could not be found
			if(!optUser.isPresent()) {
				request.logger.debug("User fould not be found").error("ID="+userId);
				request.sendError("not_found");
				return;
			}
			
			// Creates the response
			JSONObject response = new JSONObject();
			optUser.get().save(response, UserEntity.ATTRIBUTE_LIST);
			
			// Sends back the user
			request.sendResponse(response);
			
		} catch (SQLException e) {
			this.sendErrorDatabase(request, e);
		} catch (EntitySaveException e) {
			this.sendErrorUnknownException(request, e);
		}
	}

}

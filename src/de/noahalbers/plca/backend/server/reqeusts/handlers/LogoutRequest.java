package de.noahalbers.plca.backend.server.reqeusts.handlers;

import java.io.IOException;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Optional;

import org.json.JSONObject;

import de.noahalbers.plca.backend.database.entitys.TimespentEntity;
import de.noahalbers.plca.backend.database.exceptions.EntitySaveException;
import de.noahalbers.plca.backend.server.reqeusts.Permissions;
import de.noahalbers.plca.backend.server.reqeusts.Request;
import de.noahalbers.plca.backend.server.reqeusts.RequestHandler;

public class LogoutRequest extends RequestHandler{

	/**
	 * Responses:
	 * 	Errors:
	 * 		id: The client has not provided a valid id or hasn't provided an id
	 * 		stop: The client has not provided a valid time to stop or hasn't provided any time to stop
	 * 		database: Backend failed to establish a valid database connection
	 * 		user: There is no user with the given id
	 * 		unauthorized: this state is currently not authorized for the user as he is still logged in (Log out first)
	 * 		unknown: an unknown error occurre. This should not happen. Please try again.	
	 * 
	 * 	Success: Empty (User got logged in successfully)
	 * 	
	 * Request:
	 * 		id: the id of the timespent entity (Received using the getstatus request)
	 * 
	 */
	
	@Override
	public int getRequiredPermissions() {
		return Permissions.DEFAULT_LOGIN;
	}

	@Override
	public void execute(Request request) throws IOException {
		try {
			// Gets the user id
			Integer uid = request.getFromMessage("id",Integer.class);
			if(uid==null) {
				this.sendErrorMissingField(request, "id");
				return;
			}
			
			// Checks if the user exists
			if(!this.database.doesUserExists(request.startDatabaseConnection(), uid)) {
				// Log
				this.logger.debug(request+"User with id: "+uid+" not found.");
				// Sends back the error
				request.sendError("user");
				return;
			}
			
			// Tries to get the last open entity
			Optional<TimespentEntity> optEnt = this.database.getLastOpenTimespent(request.startDatabaseConnection(), uid);
			
			// Checks if the user is still logged in
			if(!optEnt.isPresent()) {
				// Log
				this.logger.debug(request+"User(id."+uid+") is not logged in. This reqeust is therefore not authorized.");
				// Sends an logged in error back
				request.sendError("unauthorized");
				return;
			}
			
			// Gets the timespent entity
			TimespentEntity ent = optEnt.get();
			
			// Updates the time
			ent.stopTime = new Timestamp(System.currentTimeMillis());
			
			// Updates the timespent entity on the database
			this.database.updateTimespent(request.startDatabaseConnection(), ent);
			
			// Log
			this.logger.debug(request+"User(id."+uid+") got logged out successfully.");
			
			// Sends back the successful result
			request.sendResponse(new JSONObject());
		} catch(SQLException e) {
			this.sendErrorDatabase(request, e);
		} catch (EntitySaveException e) {
			this.sendErrorUnknownException(request, e);
		}
	}

}

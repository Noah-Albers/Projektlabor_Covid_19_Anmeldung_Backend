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

public class LoginRequest extends RequestHandler{

	/**
	 * Responses:
	 * 	Errors:
	 * 		id: The client has not provided a valid id or hasn't provided an id
	 * 		database: Backend failed to establish a valid database connection
	 * 		user: There is no user with the given id
	 * 		unauthorized: this state is currently not authorized for the user as he is still logged in (Log out first)
	 * 		unknown: an unknown error occurre. This should not happen. Please try again.	
	 * 
	 * 	Success: Empty (User got logged in successfully)
	 * 	
	 * Request: Empty
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
			Integer uid = request.getFromMessage("id");
			if(uid == null) {
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
			if(optEnt.isPresent()) {
				// Log
				this.logger.debug(request+"User(id."+uid+") is still logged in. This reqeust is therefore not authorized.");
				// Sends an logged in error back
				request.sendError("unauthorized");
				return;
			}
			
			// Creates a new timespent entity
			TimespentEntity ts = new TimespentEntity() {{
				this.startTime=new Timestamp(System.currentTimeMillis());
				this.userId=uid;
			}};

			// Saves the timespent into the database
			this.database.createTimespent(request.startDatabaseConnection(), ts);
			
			// Log
			this.logger.debug("User(id."+uid+") got logged in successfully");
			
			// Sends the successful login
			request.sendResponse(new JSONObject());
			
		} catch(SQLException e) {
			this.sendErrorDatabase(request, e);
		} catch (IllegalStateException|EntitySaveException e) {
			// Those exception should both not occurre
			this.sendErrorUnknwonException(request, e);
		}
	}

}

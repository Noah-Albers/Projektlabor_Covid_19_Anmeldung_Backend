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
	 * 		unknown: an unknown error occurred. This should not happen. Please try again.	
	 * 
	 * 	Success: Empty (User got logged in successfully)
	 * 	
	 * Request:
	 * 	id: user id
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
			if(uid == null) {
				request.logger.debug("Provided no id");
				this.sendErrorMissingField(request, "id");
				return;
			}
			
			// Checks if the user exists
			if(!this.database.doesUserExists(request.startDatabaseConnection(), uid)) {
				// Log
				request.logger
				.debug("User not found.")
				.critical("ID="+uid);
				// Sends back the error
				request.sendError("user");
				return;
			}
			
			// Tries to get the last open entity
			Optional<TimespentEntity> optEnt = this.database.getLastOpenTimespent(request.startDatabaseConnection(), uid);
			
			// Checks if the user is still logged in
			if(optEnt.isPresent()) {
				// Log
				request.logger
				.debug("Invalid request, user is still logged in.")
				.critical("ID="+uid);
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
						
			// Sends the successful login
			request.sendResponse(new JSONObject());
			
			request.logger
			.debug("Successfully finished request")
			.critical("ID="+uid);
			
		} catch(SQLException e) {
			this.sendErrorDatabase(request, e);
		} catch (IllegalStateException|EntitySaveException e) {
			// Those exception should both not occurre
			this.sendErrorUnknownException(request, e);
		}
	}

}

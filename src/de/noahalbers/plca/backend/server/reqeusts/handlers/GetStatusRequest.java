package de.noahalbers.plca.backend.server.reqeusts.handlers;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Optional;

import org.json.JSONObject;

import de.noahalbers.plca.backend.database.entitys.TimespentEntity;
import de.noahalbers.plca.backend.server.reqeusts.Permissions;
import de.noahalbers.plca.backend.server.reqeusts.Request;
import de.noahalbers.plca.backend.server.reqeusts.RequestHandler;

public class GetStatusRequest extends RequestHandler{

	/**
	 * Responses:
	 * 	Errors:
	 * 		id: The Request failed to deliver an id or the given key for id is not an integer.
	 * 		database: Backend failed to establish a valid database connection
	 * 		user: the given id does not correspont to any user in our system
	 * 	
	 * 	Success:
	 * 		loggedin: true/false
	 * 		if logged in:
	 * 			start: the timestamp when the user logged in
	 * 	
	 * Request:
	 * 	id: Valid-Userid
	 * 
	 */
	
	@Override
	public int getRequiredPermissions() {
		return Permissions.DEFAULT_LOGIN;
	}

	@Override
	public void execute(Request request) throws IOException {
		try {
			// Gets the id
			Integer id = request.getFromMessage("id",Integer.class);
			if(id==null) {
				
				request.logger
				.debug("Id not found")
				.critical("ID="+id);
				
				this.sendErrorMissingField(request, "id");
				return;
			}
			
			// Checks if the provided user exists
			if(!this.database.doesUserExists(request.startDatabaseConnection(), id)) {
				// Log
				request.logger.debug("Request provided a user id that does not correspont to any user.");
				// Sends back the error
				request.sendError("user");
				return;
			}
			
			// Tries to get the last open entity
			Optional<TimespentEntity> optEnt = this.database.getLastOpenTimespent(request.startDatabaseConnection(), id);
			
			// Creates the status packet
			JSONObject status = new JSONObject();
			status.put("loggedin", optEnt.isPresent());
			if(optEnt.isPresent()) {
				status.put("start", optEnt.get().startTime);
				status.put("id", optEnt.get().id);
			}
			
			// Sends back the status
			request.sendResponse(status);
			
			request.logger.debug("Successfully finished request");
		} catch (SQLException e) {
			this.sendErrorDatabase(request,e);
		}
	}

}

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
	 * 		id: The Request ailed to deliver an id or the given key for id is not an integer.
	 * 		database: Backend failed to establish a valid database connection
	 * 		user: the given id does not correspont to any user in our system
	 * 	
	 * 	Success:
	 * 		loggedin: true/false if the user with the given id
	 * 		if logged in:
	 * 			start: the timestamp when the user logged in
	 * 			id: the timespent id to update later
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
			Integer id = request.getFromMessage("id");
			if(id==null) {
				this.sendErrorMissingField(request, "id");
				return;
			}
			
			// Checks if the provided user exists
			if(!this.database.doesUserExists(request.startDatabaseConnection(), id)) {
				// Log
				this.logger.debug(request+"Request provided a user id that does not correspont to any user.");
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
			
			// Log
			this.logger.debug(request+"successfully requested the status for User(id."+id+")");
			
			// Sends back the status
			request.sendResponse(status);
			
		} catch (SQLException e) {
			this.sendErrorDatabase(request,e);
		}
	}

}

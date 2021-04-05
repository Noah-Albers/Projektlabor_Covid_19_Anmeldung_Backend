package de.noahalbers.plca.backend.server.reqeusts.handlers;

import java.io.IOException;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Map.Entry;

import org.json.JSONObject;

import de.noahalbers.plca.backend.database.entitys.SimpleUserEntity;
import de.noahalbers.plca.backend.database.entitys.TimespentEntity;
import de.noahalbers.plca.backend.database.exceptions.EntitySaveException;
import de.noahalbers.plca.backend.server.reqeusts.Permissions;
import de.noahalbers.plca.backend.server.reqeusts.Request;
import de.noahalbers.plca.backend.server.reqeusts.RequestHandler;

public class LoginRFIDRequest extends RequestHandler{

	/**
	 * Responses:
	 * 	Errors:
	 * 		rfid: The client has not provided a valid rfid or hasn't provided an rfid
	 * 		user: There is no user with the given rfid
	 * 		database: Backend failed to establish a valid database connection
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
		
		// Gets the rfid
		String rfid = request.getFromMessage("rfid", String.class);
		if(rfid==null) {
			// Log
			this.logger.debug(request+"User not found");
			request.sendError("rfid");
			return;
		}
		
		try {
			// Tries to get the user and timespent
			Entry<SimpleUserEntity, TimespentEntity> res = this.database.getUserByRFID(rfid, request.startDatabaseConnection());
			
			// Checks if the user could not be found
			if(res.getKey() == null) {
				this.logger
				.debug(request+"User not found")
				.critical(request+"RFID="+rfid);
				request.sendError("user");
				return;
			}
			
			// Checks if there is no open timespent on the database
			if(res.getValue() == null) {
				// Creates a new timespent
				TimespentEntity ts = new TimespentEntity() {{
					startTime=new Timestamp(System.currentTimeMillis());
					userId=res.getKey().id;
				}};
				
				// Inserts the new timespent into the database
				this.database.createTimespent(request.startDatabaseConnection(), ts);
				
				// Sends the response (Logged in)
				request.sendResponse(new JSONObject() {{
					put("status", true);
				}});
				
				this.logger.debug(request+"Successfully finished request: login");
				
			}else {
				// Gets the timespent
				TimespentEntity ts = res.getValue();
				
				// Updates the values
				ts.stopTime = new Timestamp(System.currentTimeMillis());
				ts.gotDisconnected=false;
				
				// Updates the timespent
				this.database.updateTimespent(request.startDatabaseConnection(), ts);

				// Sends the response (Logged out)
				request.sendResponse(new JSONObject() {{
					put("status", false);
				}});
				
				this.logger.debug(request+"Successfully finished request: logout");
			}
			
			
		} catch (SQLException e) {
			this.sendErrorDatabase(request, e);
		}catch(EntitySaveException e) {
			this.sendErrorUnknownException(request, e);
		}
	}

}

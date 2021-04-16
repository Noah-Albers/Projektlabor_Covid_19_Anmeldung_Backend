package de.noahalbers.plca.backend.server.reqeusts.handlers;

import java.io.IOException;
import java.sql.SQLException;

import org.json.JSONObject;

import de.noahalbers.plca.backend.server.reqeusts.Permissions;
import de.noahalbers.plca.backend.server.reqeusts.Request;
import de.noahalbers.plca.backend.server.reqeusts.RequestHandler;

public class LogoutAllRequest extends RequestHandler{

	/**
	 * Responses:
	 * 	Errors:
	 * 		database: Backend failed to establish a valid database connection
	 * 		
	 * 	Success:
	 * 		amount: int - how many users got logged out
	 * 	
	 * Request: Empty
	 */
	
	@Override
	public int getRequiredPermissions() {
		return Permissions.DEFAULT_LOGIN;
	}

	@Override
	public void execute(Request request) throws IOException {
		try {
			// Logs out all users and gets how many got logged out
			int loggedOut = this.database.logoutAllUsers(request.startDatabaseConnection());
			
			// Sends back the successfully execution
			request.sendResponse(new JSONObject() {{
				put("amount",loggedOut);
			}});
		} catch (SQLException e) {
			this.sendErrorDatabase(request, e);
		}
	}

}

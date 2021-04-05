package de.noahalbers.plca.backend.server.reqeusts.handlers;

import java.io.IOException;
import java.sql.SQLException;

import org.json.JSONObject;

import de.noahalbers.plca.backend.database.entitys.SimpleUserEntity;
import de.noahalbers.plca.backend.database.exceptions.EntitySaveException;
import de.noahalbers.plca.backend.server.reqeusts.Permissions;
import de.noahalbers.plca.backend.server.reqeusts.Request;
import de.noahalbers.plca.backend.server.reqeusts.RequestHandler;;

public class GrabUsersRequest extends RequestHandler{

	/**
	 * Responses:
	 * 	Errors:
	 * 		database: Backend failed to establish a valid database connection
	 * 		unknown: Some unknown error occurred, please try again.
	 * 	
	 * 	Success:
	 * 		users: List with all users in the system (Simple: Firstname, Lastname, ID)
	 * 	
	 * Request: Empty
	 * 
	 */
	
	@Override
	public void execute(Request request) throws IOException {
		try {
			// Grabs all user profiles (Simple profiles)
			SimpleUserEntity[] dbUsers = this.database.getSimpleUsersFromDatabase(request.startDatabaseConnection());
			
			// Will hold all users
			JSONObject jsonUsers = new JSONObject();

			// Appends all users to the list
			for(SimpleUserEntity u : dbUsers) {
				// Converts the user to json
				JSONObject o = new JSONObject();
				u.save(o, SimpleUserEntity.DB_ENTRY_LIST);
				
				// Appends the user
				jsonUsers.append("users",o);
			}
			
			// Sends back all found users
			request.sendResponse(jsonUsers);
			
		} catch (SQLException e) {
			this.sendErrorDatabase(request,e);
		} catch (EntitySaveException e) {
			this.sendErrorUnknownException(request, e);
		}
	}

	@Override
	public int getRequiredPermissions() {
		return Permissions.DEFAULT_LOGIN;
	}

}

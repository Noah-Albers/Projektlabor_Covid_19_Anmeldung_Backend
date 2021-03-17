package de.noahalbers.plca.backend.server.reqeusts.handlers;

import java.io.IOException;
import java.sql.SQLException;

import org.json.JSONObject;

import de.noahalbers.plca.backend.database.entitys.SimpleUserEntity;
import de.noahalbers.plca.backend.server.reqeusts.Permissions;
import de.noahalbers.plca.backend.server.reqeusts.Request;
import de.noahalbers.plca.backend.server.reqeusts.RequestHandler;

public class GrabUsersRequest extends RequestHandler{

	@Override
	public void execute(Request request) throws IOException {
		try {
			// Grabs all user profiles (Simple profiles)
			SimpleUserEntity[] dbUsers = this.database.getSimpleUsersFromDatabase(request.startDatabaseConnection());
			
			// Will hold all users
			JSONObject jsonUsers = new JSONObject();

			// Appends all users to the list
			for(SimpleUserEntity u : dbUsers)
				jsonUsers.accumulate("users",new JSONObject(u));
			
			// Sends back all found users
			request.sendResponse(jsonUsers);
			
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public int getRequiredPermissions() {
		return Permissions.DEFAULT_LOGIN;
	}

}

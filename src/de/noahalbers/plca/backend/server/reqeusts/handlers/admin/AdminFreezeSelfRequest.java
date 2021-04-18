package de.noahalbers.plca.backend.server.reqeusts.handlers.admin;

import java.io.IOException;
import java.sql.SQLException;

import de.noahalbers.plca.backend.server.reqeusts.Permissions;
import de.noahalbers.plca.backend.server.reqeusts.Request;
import de.noahalbers.plca.backend.server.reqeusts.RequestHandler;
import de.noahalbers.plca.backend.server.reqeusts.checks.RequestCheck;
import de.noahalbers.plca.backend.server.reqeusts.checks.RequestChecks;

public class AdminFreezeSelfRequest extends RequestHandler{

	/**
	 * Responses:
	 * 	Errors:
	 * 		database: anything failed with the connection
	 * 
	 * 	Success: Empty
	 */
	
	@Override
	public RequestCheck[] getChecks() {
		return of(
			RequestChecks.CHECK_ADMIN_AUTH_CODE,
			RequestChecks.CHECK_ADMIN_NOT_FROZEN
		);
	}
	
	@Override
	public int getRequiredPermissions() {
		return Permissions.ADMIN;
	}

	@Override
	public void execute(Request request) throws IOException {
		try {
			// Updates the status
			this.database.freezeAdminAccount(request.startDatabaseConnection(), request.getAdmin().get().id);
			
			// Sends the successfull request back
			request.sendResponse(null);
		} catch (SQLException e) {
			this.sendErrorDatabase(request, e);
		}
	}

}

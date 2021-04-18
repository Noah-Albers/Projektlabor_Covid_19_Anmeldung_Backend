package de.noahalbers.plca.backend.server.reqeusts.handlers.admin;

import java.io.IOException;

import org.json.JSONObject;

import de.noahalbers.plca.backend.database.entitys.AdminEntity;
import de.noahalbers.plca.backend.database.exceptions.EntitySaveException;
import de.noahalbers.plca.backend.server.reqeusts.Permissions;
import de.noahalbers.plca.backend.server.reqeusts.Request;
import de.noahalbers.plca.backend.server.reqeusts.RequestHandler;
import de.noahalbers.plca.backend.server.reqeusts.checks.RequestCheck;
import de.noahalbers.plca.backend.server.reqeusts.checks.RequestChecks;

public class AdminGetProfileRequest extends RequestHandler{
	
	/**
	 * Responses:
	 * 	Errors:
	 * 		unknown: an unknown error occurred. This should not happen. Please try again.	
	 * 
	 * 	Success: Loadable json-object with all values from ENTRY_LIST
	 */
	
	private static final String[] ENTRY_LIST = {
		AdminEntity.ID,
		AdminEntity.AUTH_CODE,
		AdminEntity.IS_FROZEN,
		AdminEntity.NAME,
		AdminEntity.PERMISSIONS
	};
	
	@Override
	public RequestCheck[] getChecks() {
		return of(
			RequestChecks.CHECK_ADMIN_AUTH_CODE
		);
	}
	
	@Override
	public int getRequiredPermissions() {
		return Permissions.ADMIN;
	}

	@Override
	public void execute(Request request) throws IOException {
		try {
			// Gets the admin
			JSONObject admJson = new JSONObject();
			request.getAdmin().get().save(admJson, ENTRY_LIST);
			
			// Send the response
			request.sendResponse(admJson);
		} catch (EntitySaveException e) {
			this.sendErrorUnknownException(request, e);
			return;
		}
	}

}

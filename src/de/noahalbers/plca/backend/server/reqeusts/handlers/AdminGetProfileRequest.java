package de.noahalbers.plca.backend.server.reqeusts.handlers;

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
	
	private static final String[] ENTRY_LIST = {
		AdminEntity.ID,
		AdminEntity.AUTH_CODE,
		AdminEntity.IS_FROZEN,
		AdminEntity.NAME,
		AdminEntity.PERMISSIONS
	};
	
	@Override
	public RequestCheck[] getChecks() {
		return new RequestCheck[]{RequestChecks.ADMIN_AUTH_CODE};
	}
	
	@Override
	public int getRequiredPermissions() {
		return Permissions.ADMIN;
	}

	@Override
	public void execute(Request request) throws IOException {
		// Will holds the full admin
		JSONObject admJson = new JSONObject();
		try {
			request.getAdmin().get().save(admJson, ENTRY_LIST);
		} catch (EntitySaveException e) {
			this.sendErrorUnknownException(request, e);
			return;
		}
	}

}

package de.noahalbers.plca.backend.server.reqeusts.checks;

import java.io.IOException;
import java.util.Map.Entry;

import org.json.JSONObject;

import de.noahalbers.plca.backend.server.reqeusts.Request;
import de.noahalbers.plca.backend.util.Nullable;

@FunctionalInterface
public interface PermissionCheck{
	@Nullable
	public Entry<String,JSONObject> checkRequest(Request request) throws IOException;
}
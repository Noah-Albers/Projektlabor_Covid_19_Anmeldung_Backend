package de.noahalbers.plca.backend.server.reqeusts.handlers;

import java.io.IOException;

import org.json.JSONObject;

import de.noahalbers.plca.backend.server.reqeusts.Request;
import de.noahalbers.plca.backend.server.reqeusts.RequestHandler;

public class TestRequest extends RequestHandler{

	@Override
	public void execute(Request request) throws IOException {
		System.out.println("Received test-request");
		request.sendResponse(new JSONObject("{\"test\":\"success\"}"));
	}

}

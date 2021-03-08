package de.noahalbers.plca.backend.server.reqeusts;

import java.io.IOException;

public abstract class RequestHandler {
	
	/**
	 * Executes once a request gets received
	 * @param request the request to interact with the send data and to send or receive new data
	 * @throws IOException if anything went wrong with the I/O
	 */
	public abstract void execute(Request request) throws IOException;
	
}

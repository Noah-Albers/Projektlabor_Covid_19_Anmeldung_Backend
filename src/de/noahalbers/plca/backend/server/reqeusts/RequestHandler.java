package de.noahalbers.plca.backend.server.reqeusts;

import java.io.IOException;

import de.noahalbers.plca.backend.PLCA;
import de.noahalbers.plca.backend.logger.Logger;

public abstract class RequestHandler {
	
	// Reference to the logger
	protected Logger logger = PLCA.getInstance().getLogger();
	
	/**
	 * Executes once a request gets received
	 * @param request the request to interact with the send data and to send or receive new data
	 * @throws IOException if anything went wrong with the I/O
	 */
	public abstract void execute(Request request) throws IOException;
	
}

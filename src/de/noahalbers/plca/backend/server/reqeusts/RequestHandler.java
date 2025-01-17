package de.noahalbers.plca.backend.server.reqeusts;

import java.io.IOException;
import java.sql.SQLException;

import de.noahalbers.plca.backend.PLCA;
import de.noahalbers.plca.backend.database.PLCADatabase;
import de.noahalbers.plca.backend.server.reqeusts.checks.PermissionCheck;

public abstract class RequestHandler {

	// Reference to the database
	protected PLCADatabase database = PLCA.getInstance().getDatabase();

	/**
	 * All checks that must be performed before the request can be processed
	 * @return
	 */
	public abstract PermissionCheck[] getPermissionChecks();

	/**
	 * Executes once a request gets received
	 * 
	 * @param request
	 *            the request to interact with the send data and to send or receive
	 *            new data
	 * @throws IOException
	 *             if anything went wrong with the I/O
	 */
	public abstract void execute(Request request) throws IOException;

	/**
	 * Sends a database error and logs it
	 * 
	 * @param req
	 *            the request
	 * @throws IOException
	 *             forward
	 */
	protected void sendErrorDatabase(Request req, SQLException e) throws IOException {
		// Log
		req.logger
		.debug("Failed to open a database connection")
		.critical(e);
		// Sends back an connection error
		req.sendError("database");
	}

	/**
	 * Sends an unknown exception error
	 * 
	 * @param req
	 *            the request
	 * @param e
	 *            the exception (Used to log it)
	 * @throws IOException
	 *             forward
	 */
	protected void sendErrorUnknownException(Request req, Exception e) throws IOException {
		// Log
		req.logger
		.warn("Unknown exception occured")
		.critical(e);
		// Sends back an error
		req.sendError("unknown");
	}

	/**
	 * Sends a missing field error for the given field
	 * 
	 * @param req
	 *            the request
	 * @param name
	 *            the name of the field
	 * @throws IOException
	 *             forward
	 */
	protected void sendErrorMissingField(Request req, String name) throws IOException {
		// Log
		req.logger
		.debug("Failed to send field=" + name);
		// Sends back the error
		req.sendError(name);
	}
	
	// Method to simpler get an array
	protected PermissionCheck[] of(PermissionCheck...checks) {
		return checks;
	}
}

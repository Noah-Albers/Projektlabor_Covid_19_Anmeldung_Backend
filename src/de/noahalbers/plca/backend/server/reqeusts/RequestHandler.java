package de.noahalbers.plca.backend.server.reqeusts;

import java.io.IOException;
import java.sql.SQLException;

import de.noahalbers.plca.backend.PLCA;
import de.noahalbers.plca.backend.database.PLCADatabase;
import de.noahalbers.plca.backend.logger.Logger;

public abstract class RequestHandler {

	// Reference to the logger
	protected Logger logger = PLCA.getInstance().getLogger();

	// Reference to the database
	protected PLCADatabase database = PLCA.getInstance().getDatabase();

	/**
	 * @return an int that indicates the required permissions to access the request
	 *         handler. This int is encoded using simple binary or. Used
	 *         Permission_1 | Permissions_2 to make multiple users be allowed to
	 *         access the handler.
	 */
	public abstract int getRequiredPermissions();

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
	public void sendErrorDatabase(Request req, SQLException e) throws IOException {
		// Log
		this.logger
		.debug(req + "Failed to open a database connection")
		.critical(req.toString()+e);
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
	public void sendErrorUnknownException(Request req, Exception e) throws IOException {
		// Log
		this.logger
		.warn(req + "Unknown exception occured")
		.critical(req.toString()+e);
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
	public void sendErrorMissingField(Request req, String name) throws IOException {
		// Log
		this.logger
		.debug(req + "Failed to send field=" + name);
		// Sends back the error
		req.sendError(name);
	}
}

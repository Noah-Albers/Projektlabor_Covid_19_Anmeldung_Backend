package de.noahalbers.plca.backend.server.reqeusts;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Optional;

import javax.annotation.Nullable;

import org.json.JSONObject;

import de.noahalbers.plca.backend.PLCA;
import de.noahalbers.plca.backend.database.entitys.AdminEntity;

public class Request {

	// Reference to the main app
	private PLCA plca = PLCA.getInstance();

	// Control funtions for the socket
	private PacketSender doSend;
	private PacketReceiver doReceive;
	// A database connection that can be reused
	private @Nullable Connection dbConnection;
	// The requesting admin, in found
	private @Nullable AdminEntity admin;

	// The json message that got send
	private JSONObject message;
	
	// Request id that can is used for debugging
	private long requestID;

	public Request(long requestID,JSONObject message, PacketSender doSend, PacketReceiver doReceive, @Nullable Connection dbConnection,
			@Nullable AdminEntity admin) {
		this.requestID=requestID;
		this.message = message;
		this.doSend = doSend;
		this.doReceive = doReceive;
		this.dbConnection = dbConnection;
		this.admin = admin;
	}

	/**
	 * Sends a response to the remote client
	 * 
	 * @param data
	 *            the data to send
	 * @throws IOException
	 *             if anything went wrong with the I/O
	 */
	public void sendResponse(JSONObject data) throws IOException {
		// Creates the response
		JSONObject response = new JSONObject() {{
			// Appends the success status
			this.accumulate("status", 1);
			// Appends the data
			this.accumulate("data", data);
		}};

		// Sends the response
		this.doSend.send(response);
	}

	/**
	 * Sends an error back to the client
	 * 
	 * @param code
	 *            the error code that the client can deal with
	 * @param infos
	 *            addition infos regarding the error
	 * @throws IOException
	 *             if anything happens with the I/O
	 */
	public void sendError(String code, @Nullable JSONObject infos) throws IOException {
		// Creates the response
		JSONObject response = new JSONObject() {
			{
				// Appends the error status
				this.accumulate("status", 0);
				// Appends the error status
				this.accumulate("errorcode", code);

				// Checks if additional infos got parsed
				if (infos != null)
					// Appends the error infos
					this.accumulate("data", infos);
			}
		};

		// Sends the response
		this.doSend.send(response);
	}
	
	/**
	 * Sends an error back to the client
	 * 
	 * @param code
	 *            the error code that the client can deal with
	 * @throws IOException
	 *             if anything happens with the I/O
	 */
	public void sendError(String code) throws IOException {
		this.sendError(code, null);
	}

	/**
	 * Waits for a response/packet to be send
	 * 
	 * @return the raw data (Unencrypted) that got send
	 * @throws IOException
	 *             if anything went wrong with the I/O (Timeout maybe)
	 */
	public JSONObject waitForResponse() throws IOException {
		return this.doReceive.receive();
	}

	public JSONObject getMessage() {
		return this.message;
	}

	/**
	 * Gets an object from the provided message
	 * 
	 * @param key
	 *            the key for the object
	 * @return the object if provided; null if either no object got provided or the
	 *         object had the wrong data-type
	 */
	@SuppressWarnings("unchecked")
	@Nullable
	public <T> T getFromMessage(String key) {
		try {
			return (T) this.message.get(key);
		} catch (Exception e) {
			return null;
		}
	}

	/**
	 * @return optionally the admin if an admin is requesting
	 */
	@Nullable
	public Optional<AdminEntity> getAdmin() {
		return Optional.ofNullable(this.admin);
	}

	/**
	 * Reuses or starts a new database connection
	 * 
	 * @return
	 * @throws SQLException
	 *             if the connection failed to start
	 */
	public Connection startDatabaseConnection() throws SQLException {
		// Checks if no connection is currently open
		if (this.dbConnection == null)
			return this.plca.getDatabase().startConnection();
		return this.dbConnection;
	}

	/**
	 * Deletes the object and destructs it
	 */
	public void Destruct() {
		// Checks if the database connection still is open
		if (this.dbConnection != null) {
			try {
				// Closes the db-connection
				this.dbConnection.close();
			} catch (SQLException e) {
			}
			this.dbConnection = null;
		}
	}

	@Override
	public String toString() {
		return String.format("Request(id.%d) ", this.requestID);
	}
	
	@FunctionalInterface
	public interface PacketSender {
		public void send(JSONObject data) throws IOException;
	}

	@FunctionalInterface
	public interface PacketReceiver {
		public JSONObject receive() throws IOException;
	}
}

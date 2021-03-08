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
	
	public Request(JSONObject message,PacketSender doSend, PacketReceiver doReceive,
			@Nullable Connection dbConnection, @Nullable AdminEntity admin) {
		this.message = message;
		this.doSend = doSend;
		this.doReceive = doReceive;
		this.dbConnection = dbConnection;
		this.admin = admin;
	}
	
	/**
	 * Reuses or starts a new database connection
	 * @return
	 * @throws SQLException if the connection failed to start
	 */
	public Connection startDatabaseConnection() throws SQLException {
		// Checks if no connection is currently open
		if(this.dbConnection == null)
			return this.plca.getDatabase().startConnection();
		return this.dbConnection;
	}
	
	/**
	 * @return optionally the admin if an admin is requesting
	 */
	@Nullable
	public Optional<AdminEntity> getAdmin() {
		return Optional.ofNullable(this.admin);
	}
	
	/**
	 * Sends a response to the remote client
	 * @param data the data to send
	 * @throws IOException if anything went wrong with the I/O
	 */
	public void sendResponse(JSONObject data) throws IOException{
		this.doSend.send(data);
	}
	
	/**
	 * Waits for a response/packet to be send
	 * @return the raw data (Unencrypted) that got send
	 * @throws IOException if anything went wrong with the I/O (Timeout maybe)
	 */
	public JSONObject waitForResponse() throws IOException{
		return this.doReceive.receive();
	}

	public JSONObject getMessage() {
		return this.message;
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

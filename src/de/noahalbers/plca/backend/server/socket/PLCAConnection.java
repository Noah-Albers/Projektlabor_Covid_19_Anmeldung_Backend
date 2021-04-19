package de.noahalbers.plca.backend.server.socket;

import java.io.IOException;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.PublicKey;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.function.Consumer;

import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.json.JSONException;
import org.json.JSONObject;

import com.mysql.cj.exceptions.RSAException;

import de.noahalbers.plca.backend.EncryptionManager;
import de.noahalbers.plca.backend.PLCA;
import de.noahalbers.plca.backend.database.entitys.AdminEntity;
import de.noahalbers.plca.backend.logger.Logger;
import de.noahalbers.plca.backend.server.reqeusts.Request;
import de.noahalbers.plca.backend.server.reqeusts.RequestHandler;
import de.noahalbers.plca.backend.server.reqeusts.checks.PermissionCheck;
import de.noahalbers.plca.backend.server.socket.exception.PLCAAdminNotFoundException;
import de.noahalbers.plca.backend.server.socket.exception.PLCAConnectionTimeoutException;
import de.noahalbers.plca.backend.util.Nullable;

public class PLCAConnection extends Thread {

	// Reference to the program
	private PLCA plca = PLCA.getInstance();

	// Logger
	private Logger log;

	// The socket for the connection
	private PLCASocket socket;

	// Callback function for when the connection status changes
	private Consumer<ConnectionStatus> onStatusChange;

	// Manager for the encryption
	private EncryptionManager encryptionManager = new EncryptionManager();

	// A connection that has been opend to the database to extract information. May
	// be null depending on the time
	@Nullable
	private Connection dbconnection;

	// The admin that is connected. If not admin is connected, the field will be
	// null
	@Nullable
	private AdminEntity connectedAdmin;

	// The remote key after the ssl-like handshake. Will only be available after the
	// handshake.
	@Nullable
	private SecretKeySpec aesKey;
	@Nullable
	private IvParameterSpec aesIv;

	// The bytes that are required to be send when sending a response
	@Nullable
	private byte[] nonceBytes;

	public PLCAConnection(long connectionID, Socket socket, Consumer<ConnectionStatus> onStatusChange)
			throws NumberFormatException {
		this.log = new Logger("PLCAConnection." + connectionID);

		this.onStatusChange = onStatusChange;
		this.socket = new PLCASocket(connectionID, socket, this.plca.getConfig().getUnsafe("connection_timeout"));
		this.encryptionManager.init();
	}

	@Override
	public void run() {
		// Request object that shall be used
		Request request = null;

		// Performs the secure handshake with the client
		if (!this.doHandshake())
			return;

		// Now we have a secure connection an can start to send data

		try {
			// Waits for the request-packet
			JSONObject pkt = this.receivePacket();

			// Holds the specified handler
			RequestHandler handler;

			try {
				// Gets the endpoint id
				int endptId = pkt.getInt("endpoint");

				// Gets the reqeust-handler
				handler = this.plca.getServer().getHandlerById(endptId);

				// Checks if the handler exists
				if (handler == null)
					throw new JSONException("Invalid handler got specified.");

			} catch (JSONException e) {
				this.sendPreprocessingErrorAndClose("handler", null, ConnectionStatus.DISCONNECTED_NO_HANDLER);
				return;
			}

			// The data for the request
			JSONObject requestData = new JSONObject();
			try {
				requestData = pkt.getJSONObject("data");
			} catch (JSONException e) {
			}

			// An optional auth object
			JSONObject auth = new JSONObject();
			try {
				auth = pkt.getJSONObject("auth");
			} catch (JSONException e) {
			}

			// Creates the request
			request = new Request(this.socket.getConnectionId(), requestData, auth, this::sendPacket,
					this::receivePacket, this.dbconnection, this.connectedAdmin);

			// Checks that all permissions are given
			for (PermissionCheck check : handler.getPermissionChecks()) {
				// Performs permission checks
				Entry<String, JSONObject> result = check.checkRequest(request);

				// Checks if the check failed
				if (result != null) {
					this.sendPreprocessingErrorAndClose(result.getKey(), result.getValue(),
							ConnectionStatus.DISCONNECTED_AUTH_ERROR);
					return;
				}
			}

			// Executes the handler
			handler.execute(request);

			// Log
			this.log.debug("Got completed successfully. Disconnecting...");

			// Ends the connection
			this.killConnection(ConnectionStatus.DISCONNECTED_SUCCESS);
		} catch (IOException e) {
			// Log
			this.log.debug("Got disconnected (I/O Error)").critical(e.toString());
		} finally {
			// Deletes the object
			if (request != null)
				request.Destruct();
		}
	}

	/**
	 * Establishes a secure connection with the remote client
	 * 
	 * @return if the handshake was successful. If not the connection got killed
	 *         automatically.
	 */
	private boolean doHandshake() {
		try {
			// Gets the client id (0 is the covid-login)
			short clientId = this.socket.readUByte();

			// Logs
			this.log.debug("Received clientid").critical("ID=" + clientId);

			// Receives the nonce
			this.nonceBytes = this.socket.readXBytes(8);

			this.log.debug("Received nonce").critical("Nonce=" + Arrays.toString(this.nonceBytes));

			// Gets the remote rsa-public key
			PublicKey remoteKey = this.getKeyAndPrepare(clientId);

			// Generates the secret for the communication (AES-Key and AES-Init-Vector)
			this.aesKey = this.encryptionManager.generateAESKey();
			this.aesIv = this.encryptionManager.generateAESIV();

			// Holds the combined bytes of the key and iv as a packet to send
			byte[] packet = new byte[32 + 16];

			// Stores the key and iv into the packet
			System.arraycopy(this.aesKey.getEncoded(), 0, packet, 0, 32);
			System.arraycopy(this.aesIv.getIV(), 0, packet, 32, 16);

			// Encrypts the packet using the rsa-key
			Optional<byte[]> optEnc = this.encryptionManager.encryptRSA(packet, remoteKey);

			// Checks if anything went wrong with the encryption
			if (!optEnc.isPresent())
				throw new IOException("Error while encrypting with the aes-bytes.");

			// Sends the encrypted aes-key and aes-iv
			this.socket.write(optEnc.get());
			// Updates the socket
			this.socket.flush();

			// Log
			this.log.debug("Handshake was successfull");

			return true;
		} catch (IOException | PLCAAdminNotFoundException e) {
			// Log
			this.log.debug("Auth error").critical(e);
			// Kills the connection with an auth error
			this.killConnection(ConnectionStatus.DISCONNECTED_AUTH_ERROR);
		} catch (RSAException | SQLException e) {
			// Log
			this.log.warn("Auth error").critical(e);
			// Kills the connection with an auth error
			this.killConnection(ConnectionStatus.DISCONNECTED_AUTH_ERROR);
		}

		// Handshake failed for some reason
		return false;
	}

	/**
	 * Gets the remote rsa-public key for the requesting user
	 * 
	 * Can open a database connection that will be globally stored if the requesting
	 * user is an admin Can store the reqeusting admin globally if the requester is
	 * an admin
	 * 
	 * @param id
	 *            the id for the reqeust (Admin-ID or 0 for Covid-Login)
	 * @return the public rsa key from the remote user
	 * @throws SQLException
	 *             if anything went wrong with the database connection
	 * @throws RSAException
	 *             if anything went wrong with getting (generating or obtaining) the
	 *             public-key
	 * @throws PLCAAdminNotFoundException
	 *             if the admin could not be found (the id does not correlate with
	 *             any database entry's)
	 */
	private PublicKey getKeyAndPrepare(short id) throws SQLException, RSAException, PLCAAdminNotFoundException {
		// Checks if the requester is from the covid-login
		if (id == 0)
			return this.encryptionManager.getPublicKeyFromSpec(this.plca.getConfig().getUnsafe("applogin_pubK"));

		// Opens a connection to the database
		this.dbconnection = this.plca.getDatabase().startConnection();

		// Gets the logged in admin
		Optional<AdminEntity> adm = this.plca.getDatabase().getAdminById(id, this.dbconnection);

		// Checks if the admin account got found
		if (!adm.isPresent())
			throw new PLCAAdminNotFoundException();

		// Stores the admin
		this.connectedAdmin = adm.get();

		// Tries to generate the key
		return this.encryptionManager
				.getPublicKeyFromSpec(EncryptionManager.getPublicKeySpecFromJson(new JSONObject(adm.get().publicKey)));
	}

	/**
	 * Sends a packet with an pre-processing error
	 * 
	 * @param status
	 *            the status that should be used to send the connection
	 * @param error
	 *            the error string
	 * @param data
	 *            extra data that can optionally be passed with the error message
	 * @throws IOException
	 *             if anything went wrong
	 */
	private void sendPreprocessingErrorAndClose(String error, @Nullable JSONObject data, ConnectionStatus status)
			throws IOException {
		// Log
		this.log.debug("Failed in pre-processing").critical("Error=" + error + " Status=" + status);

		// Sends an packet the error
		this.sendPacket(new JSONObject() {
			{
				put("status", 2);
				put("error", error);
				put("data",data == null ? new JSONObject() : data);
			}
		});

		// Kills the connection
		this.killConnection(status);
	}

	/**
	 * Can only be used after the handshake is completed.
	 * 
	 * Waits for the next packet to be send
	 * 
	 * @return the raw packet that got received (Json)
	 * @throws IOException
	 *             if anything went wrong with the I/O or if the decryption fails
	 * @throws PLCAConnectionTimeoutException
	 *             if the connection timed out
	 */
	public JSONObject receivePacket() throws IOException {
		try {
			// Waits for the request
			int len = this.socket.readUByte() | (this.socket.readUByte() << 8);

			// Create the space for the amount of bytes that will be received
			byte[] data = new byte[len];

			// Receives the data
			this.socket.readByte(data);

			// Tries to decrypt the data
			Optional<byte[]> optDec = this.encryptionManager.decryptAES(data, this.aesKey, this.aesIv);

			// Checks if the decryption failed
			if (!optDec.isPresent())
				throw new IOException("Failed to decrypt received message: " + Arrays.toString(data));

			return new JSONObject(new String(optDec.get(), StandardCharsets.UTF_8));
		} catch (IOException e) {
			// Ensures a terminated connection
			this.killConnection(ConnectionStatus.DISCONNECTED_IO);
			throw e;
		} catch (JSONException e) {
			// Ensures a terminated connection
			this.killConnection(ConnectionStatus.DISCONNECTED_JSON);
			throw new IOException("Failed to parse json");
		}
	}

	/**
	 * Can only be used after the handshake is completed.
	 * 
	 * Sends a packet that is encrypted using the agreed key
	 * 
	 * @param data
	 *            the packet that shall be send (Json)
	 * @param key
	 *            the aes key
	 * @param iv
	 *            the aes init vector
	 * @throws IOException
	 *             if anything happens with the I/O or the encryption failes
	 */
	public void sendPacket(JSONObject data) throws IOException {
		try {
			// Gets the raw packet bytes
			byte[] rawPkt = data.toString().getBytes(StandardCharsets.UTF_8);

			// Combines the nonce bytes and the data from the jobject
			byte[] finPkt = ByteBuffer.allocate(rawPkt.length + this.nonceBytes.length).put(this.nonceBytes).put(rawPkt)
					.array();

			// Tries to encrypt the message
			Optional<byte[]> optEnc = this.encryptionManager.encryptAES(finPkt, this.aesKey, this.aesIv);

			// Checks if the encryption failed
			if (!optEnc.isPresent())
				throw new IOException("Failed to encrypt message.");

			byte[] pkt = optEnc.get();

			// Checks if an error occured with the amount of bytes that need to be send
			if (pkt.length > Math.pow(2, 16))
				this.log.error("Connection needs to send a message with " + pkt.length
						+ " bytes, but can only send a packet with " + Math.pow(2, 16) + " bytes!");

			// Sends the length for the response
			this.socket.write((byte) pkt.length);
			this.socket.write((byte) (pkt.length >> 8));

			// Sends the data
			this.socket.write(pkt);
			this.socket.flush();
		} catch (IOException e) {
			// Ensures a terminated connection
			this.killConnection(ConnectionStatus.DISCONNECTED_IO);
			throw e;
		}
	}

	/**
	 * Closes the connection and all connected objects
	 * 
	 * @param optCallback
	 *            if given, executes the callback with the given status
	 */
	private void killConnection(@Nullable ConnectionStatus optCallback) {
		// Kills the socket
		this.socket.killConnection();

		// Closes the database connection
		if (this.dbconnection != null) {
			try {
				this.dbconnection.close();
			} catch (SQLException e) {
			}
			this.dbconnection = null;
		}

		// Executes the callback if required
		if (optCallback != null)
			this.onStatusChange.accept(optCallback);
	}
}

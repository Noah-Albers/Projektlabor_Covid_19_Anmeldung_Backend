package de.noahalbers.plca.backend.server.socket;

import java.io.IOException;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.PublicKey;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Optional;
import java.util.function.Consumer;

import javax.annotation.Nullable;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.json.JSONException;
import org.json.JSONObject;

import com.mysql.cj.exceptions.RSAException;

import de.noahalbers.plca.backend.EncryptionManager;
import de.noahalbers.plca.backend.PLCA;
import de.noahalbers.plca.backend.database.entitys.AdminEntity;
import de.noahalbers.plca.backend.logger.Logger;
import de.noahalbers.plca.backend.server.reqeusts.Permissions;
import de.noahalbers.plca.backend.server.reqeusts.Request;
import de.noahalbers.plca.backend.server.reqeusts.RequestHandler;
import de.noahalbers.plca.backend.server.socket.exception.PLCAAdminNotFoundException;
import de.noahalbers.plca.backend.server.socket.exception.PLCAConnectionTimeoutException;

public class PLCAConnection extends Thread {

	// Reference to the program
	private PLCA plca = PLCA.getInstance();

	// Logger
	private Logger logger = this.plca.getLogger();

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

	public PLCAConnection(Socket socket, Consumer<ConnectionStatus> onStatusChange) throws NumberFormatException {
		this.onStatusChange = onStatusChange;
		this.socket = new PLCASocket(socket, Long.parseLong(this.plca.getConfig().get("connection_timeout")));
		this.encryptionManager.init();
	}

	@Override
	public void run() {
		try {
			// Does the handshake with the remote client to provide a secure connection
			this.doHandshake();

			// Now we have a secure connection an can start to send data

			// Waits for the request-packet
			JSONObject pkt = this.receivePacket();

			// Gets the endpoint id
			int endptId = pkt.getInt("endpoint");

			// Gets the reqeust-handler
			RequestHandler handler = this.plca.getServer().getHandlerById(endptId);

			// Checks if the handler exists
			if (handler == null) {
				// TODO: Send back error
				throw new IOException("No handler for id: " + endptId);
			}
			
			// Gets the users permissions
			int perm = this.connectedAdmin == null ? Permissions.DEFAULT_LOGIN : this.connectedAdmin.getPermissions();
			
			// Checks if the user does not have the permissions to access the app
			if((perm & handler.getRequiredPermissions()) == 0) {
				// TODO: Send back error
				throw new IOException("Requesting user has not all permissions to access");
			}
			
			// Creates the request
			Request r = new Request(
				pkt.has("data") ? pkt.getJSONObject("data") : new JSONObject(),
				this::sendPacket,
				this::receivePacket,
				this.dbconnection,
				this.connectedAdmin
			);

			// Executes the handler
			handler.execute(r);

			// Ends the connection
			this.killConnection(ConnectionStatus.DISCONNECTED_SUCCESS);
		} catch (Exception e) {
			e.printStackTrace();
			// TODO: handle different exceptions
			this.killConnection(ConnectionStatus.DISCONNECTED_AUTH_ERROR);
			return;
		}
	}

	/**
	 * Establishes a secure connection with the remote client
	 * 
	 * @throws IOException
	 *             if anything happens with the I/O
	 * @throws RSAException
	 *             if there is an exception with the encryption
	 * @throws SQLException
	 *             if there is an exception with the sql-server
	 * @throws PLCAAdminNotFoundException
	 *             if the admin could not be found
	 */
	private void doHandshake() throws IOException, RSAException, SQLException, PLCAAdminNotFoundException {
		try {
			// Gets the client id (0 is the covid-login)
			short clientId = this.socket.readUByte();

			// Logs the client id
			this.logger.debug("Received clientid: " + clientId);

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
			if (!optEnc.isPresent()) {
				this.logger.error("Error while encrypting the aes-bytes");
				throw new IOException("Error while encrypting with the aes-bytes.");
			}

			// Sends the encrypted aes-key and aes-iv
			this.socket.write(optEnc.get());
			// Updates the socket
			this.socket.flush();
		} catch (IOException | RSAException | SQLException | PLCAAdminNotFoundException e) {
			e.printStackTrace();
			this.killConnection(ConnectionStatus.DISCONNECTED_AUTH_ERROR);
		}
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
			return this.encryptionManager.getPublicKeyFromSpec(EncryptionManager
					.getPublicKeySpecFromJson(new JSONObject(this.plca.getConfig().get("applogin_pubK"))));

		// Opens a connection to the database
		this.dbconnection = this.plca.getDatabase().startConnection();

		// Gets the logged in admin
		Optional<AdminEntity> adm = this.plca.getDatabase().getAdminById(1, this.dbconnection);

		// Checks if the admin account got found
		if (!adm.isPresent())
			throw new PLCAAdminNotFoundException();

		// Stores the admin
		this.connectedAdmin = adm.get();

		// Tries to generate the key
		return this.encryptionManager.getPublicKeyFromSpec(adm.get().getPublicKeySpec());
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
			// Tries to encrypt the message
			Optional<byte[]> optEnc = this.encryptionManager
					.encryptAES(data.toString().getBytes(StandardCharsets.UTF_8), this.aesKey, this.aesIv);

			// Checks if the encryption failed
			if (!optEnc.isPresent())
				throw new IOException("Failed to encrypt message.");

			byte[] pkt = optEnc.get();

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
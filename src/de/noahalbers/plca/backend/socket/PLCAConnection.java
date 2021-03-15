package de.noahalbers.plca.backend.socket;

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

import org.json.JSONObject;

import com.mysql.cj.exceptions.RSAException;

import de.noahalbers.plca.backend.EncryptionManager;
import de.noahalbers.plca.backend.PLCA;
import de.noahalbers.plca.backend.database.entitys.AdminEntity;
import de.noahalbers.plca.backend.socket.exception.PLCAAdminNotFoundException;
import de.noahalbers.plca.backend.socket.exception.PLCAConnectionTimeoutException;

public class PLCAConnection extends Thread{
	
	// Reference to the program
	private PLCA plca = PLCA.getInstance();
	
	// The socket for the connection
	private PLCASocket socket;
	
	// Callback function for when the connection status changes
	private Consumer<ConnectionStatus> onStatusChange;
	
	// Manager for the encryption
	private EncryptionManager encryptionManager = new EncryptionManager();
	
	// A connection that has been opend to the database to extract information. May be null depending on the time
	@Nullable private Connection dbconnection;
	
	// The admin that is connected. If not admin is connected, the field will be null
	@Nullable private AdminEntity connectedAdmin;
	
	public PLCAConnection(Socket socket,Consumer<ConnectionStatus> onStatusChange) throws NumberFormatException {
		this.onStatusChange=onStatusChange;
		this.socket = new PLCASocket(socket, Long.parseLong(this.plca.getConfig().get("connection_timeout")));
		this.encryptionManager.init();
	}
	
	@Override
	public void run() {
		try {
			// Gets the client id (0 is the covid-login)
			short clientId = this.socket.readUByte();
			
			// TODO: Log
			System.out.println("ClientId: "+clientId);
			
			// Gets the remote rsa-public key
			PublicKey remoteKey = this.getKeyAndPrepare(clientId);
			
			// Generates the secret for the communication (AES-Key and AES-Init-Vector)
			SecretKeySpec key = this.encryptionManager.generateAESKey();
			IvParameterSpec iv = this.encryptionManager.generateAESIV();
			
			// Holds the combined bytes of the key and iv as a packet to send
			byte[] packet = new byte[32+16];
			
			// Stores the key and iv into the packet
			System.arraycopy(key.getEncoded(), 0, packet, 0, 32);
			System.arraycopy(iv.getIV(), 0, packet, 32, 16);
			
			System.out.println(Arrays.toString(packet));
			
			// Encrypts the packet using the rsa-key
			Optional<byte[]> optEnc = this.encryptionManager.encryptRSA(packet, remoteKey);
			
			// Checks if anything went wrong with the encryption
			if(!optEnc.isPresent())
				// TODO
				throw new Exception("Error while encrypting with the public-key.");
			
			// Sends the encrypted aes-key and aes-iv
			this.socket.write(optEnc.get());
			// Updates the socket
			this.socket.flush();
			
			/*
			 * Handshake is complete
			 * */
			
			// Waits for the request-packet
			byte[] pkt = this.receivePacket(key,iv);
			
			System.out.println("Received: "+new String(pkt,StandardCharsets.UTF_8));
			
			// Ends the connection
			this.killConnection(ConnectionStatus.DISCONNECTED_SUCCESS);
		} catch (Exception e) {
			e.printStackTrace();
			//TODO: handle different exceptions
			this.killConnection(ConnectionStatus.DISCONNECTED_AUTH_ERROR);
			return;
		}
	}
	
	/**
	 * Gets the remote rsa-public key for the requesting user
	 * 
	 * Can open a database connection that will be globally stored if the requesting user is an admin
	 * Can store the reqeusting admin globally if the requester is an admin
	 * 
	 * @param id the id for the reqeust (Admin-ID or 0 for Covid-Login)
	 * @return the public rsa key from the remote user
	 * @throws SQLException if anything went wrong with the database connection
	 * @throws RSAException if anything went wrong with getting (generating or obtaining) the public-key
	 * @throws PLCAAdminNotFoundException if the admin could not be found (the id does not correlate with any database entry's)
	 */
	private PublicKey getKeyAndPrepare(short id) throws SQLException,RSAException,PLCAAdminNotFoundException{
		
		// Checks if the requester is from the covid-login
		System.out.println(this.plca.getConfig().get("applogin_pubK"));
		if(id == 0)
			return this.encryptionManager.getPublicKeyFromSpec(EncryptionManager.getPublicKeySpecFromJson(new JSONObject(this.plca.getConfig().get("applogin_pubK"))));
		
		// Opens a connection to the database
		this.dbconnection = this.plca.getDatabase().startConnection();
		
		// Gets the logged in admin
		Optional<AdminEntity> adm = this.plca.getDatabase().getAdminById(1, this.dbconnection);
		
		// Checks if the admin account got found
		if(!adm.isPresent()) {
			// TODO: send error
			this.killConnection(ConnectionStatus.DISCONNECTED_AUTH_ERROR);
			throw new PLCAAdminNotFoundException();
		}
		
		// Stores the admin
		this.connectedAdmin = adm.get();
		
		// Tries to generate the key
		return this.encryptionManager.getPublicKeyFromSpec(adm.get().getPublicKeySpec());
	}
	
	/**
	 * Waits for the next packet to be send
	 * @param key the aes key for the decryption
	 * @param iv the init vector for the aes decryption
	 * @return the raw data from the packet (Encrypted)
	 * @throws IOException if anything went wrong with the I/O or if the decryption fails
	 * @throws PLCAConnectionTimeoutException if the connection timed out
	 */
	public byte[] receivePacket(SecretKeySpec key,IvParameterSpec iv) throws IOException, PLCAConnectionTimeoutException {
		// Waits for the request
		int len = this.socket.readUByte() | (this.socket.readUByte() << 8);
		
		// Create the space for the amount of bytes that will be received
		byte[] data = new byte[len];
		
		// Receives the data
		this.socket.readByte(data);
		
		// Tries to decrypt the data
		Optional<byte[]> optDec = this.encryptionManager.decryptAES(data, key, iv);
		
		// Checks if the decryption failed
		if(!optDec.isPresent())
			throw new IOException("Failed to decrypt received message: "+Arrays.toString(data));
		
		return optDec.get();
	}
	
	/**
	 * Sends a packet that is encrypted using the agreed key
	 * @param data the data to send (Cannot be more than the size of a short)
	 * @param key the aes key
	 * @param iv the aes init vector
	 * @throws IOException if anything happens with the I/O or the encryption failes
	 */
	public void sendPacket(byte[] data,SecretKeySpec key,IvParameterSpec iv) throws IOException {
		
		// Tries to encrypt the message
		Optional<byte[]> optEnc = this.encryptionManager.encryptAES(data, key, iv);
		
		// Checks if the encryption failed
		if(!optEnc.isPresent())
			throw new IOException("Failed to encrypt message.");
		
		byte[] pkt = optEnc.get();
		
		// Sends the length for the response
		this.socket.write((byte) pkt.length);
		this.socket.write((byte) (pkt.length>>8));
		
		// Sends the data
		this.socket.write(pkt);
		this.socket.flush();
	}
	
	/**
	 * Closes the connection and all connected objects
	 * @param optCallback if given, executes the callback with the given status
	 */
	private void killConnection(@Nullable ConnectionStatus optCallback) {
		// Kills the socket
		this.socket.killConnection();
		
		// Closes the database connection
		if(this.dbconnection != null) {
			try {
				this.dbconnection.close();
			} catch (SQLException e) {}
			this.dbconnection = null;
		}
		
		// Executes the callback if required
		if(optCallback != null)
			this.onStatusChange.accept(optCallback);
		
	}
}

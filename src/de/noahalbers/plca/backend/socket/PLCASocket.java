package de.noahalbers.plca.backend.socket;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

import de.noahalbers.plca.backend.Config;
import de.noahalbers.plca.backend.PLCA;

public class PLCASocket extends Thread{
	
	// Reference to the program
	private PLCA plca = PLCA.getInstance();
	
	// Gets the send timeout
	private final long sendTimeout;
	
	// Socket connection to the remote client
	private Socket socket;
	
	// Reader and writer for the connection
	private BufferedReader reader;
	private BufferedWriter writer;
	
	// Callback function for when the connection status changes
	private Consumer<ConnectionStatus> onStatusChange;
	
	// Last the that the remote client has send data (in ms)
	private long lastSendTime;
	
	public PLCASocket(Socket socket,Consumer<ConnectionStatus> onStatusChange) throws NumberFormatException {
		this.socket=socket;
		this.onStatusChange=onStatusChange;
		this.sendTimeout = Long.parseLong(this.plca.getConfig().getString("connection_timeout"));
	}
	
	@Override
	public void run() {
		// Tries to open the connection
		try {
			this.reader = new BufferedReader(new InputStreamReader(this.socket.getInputStream(), StandardCharsets.UTF_8));
			this.writer = new BufferedWriter(new OutputStreamWriter(this.socket.getOutputStream(), StandardCharsets.UTF_8));
		} catch (IOException e) {
			// Closes the socket
			this.killConnection(true);
			return;
		}
		
		// Waits the identification index
		int identityID;
		try {
			identityID = this.read();
		} catch (Exception e) {
			return;
		}
		
		// Gets the 
		
	}
	
	/**
	 * Reads the next byte from the stream and timeouts if it took to long to reach.
	 * 
	 * If an exception is thrown, the socket had already been killed
	 * @return the next byte
	 * @throws Exception if the connection had to be terminated
	 */
	private int read() throws Exception{
		// Updates the timeout
		this.lastSendTime = System.currentTimeMillis();
		
		// Waits until the timeout has been reached
		while(System.currentTimeMillis()-this.lastSendTime <= this.sendTimeout) {
			try {
				// Checks if a new byte got received
				if(this.reader.ready())
					return this.reader.read();
			} catch (IOException e) {
				this.killConnection(true);
				throw e;
			}
			
			Thread.sleep(10);
		}
		// Kills the connection
		this.killConnection(false);
		// Sends the status
		this.onStatusChange.accept(ConnectionStatus.DISCONNECTED_TIMEOUT);
		// Throws an timeout
		throw new Exception("Timeout");
	}
	
	/**
	 * Kills the socket connection
	 * 
	 * @param executeCallback if the status-callback should be called with disconnect
	 */
	public void killConnection(boolean executeCallback) {
		// Checks if the socket is still alive
		if(this.socket != null) {
			try {
				// Tries to kill the socket
				this.socket.close();
			} catch (IOException e) {}
			this.socket = null;
			
			// Checks if the status-callback should be executed
			if(executeCallback)
				this.onStatusChange.accept(ConnectionStatus.DISCONNECTED);
		}
	}
	
}

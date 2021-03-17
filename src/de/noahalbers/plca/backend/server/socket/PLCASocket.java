package de.noahalbers.plca.backend.server.socket;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

import de.noahalbers.plca.backend.server.socket.exception.PLCAConnectionTimeoutException;

public class PLCASocket {

	// Gets the send timeout
	private final long sendTimeout;
	
	// Last the that the remote client has send data (in ms)
	private long lastSendTime;
	
	// Socket connection to the remote client
	private Socket socket;
	
	// Reader and writer for the connection
	private InputStream reader;
	private OutputStream writer;
	
	public PLCASocket(Socket socket,long timeout) {
		this.socket=socket;
		this.sendTimeout = timeout;
		
		// Gets the reader and writer
		try {
			this.reader = this.socket.getInputStream();
			this.writer = this.socket.getOutputStream();
		} catch (IOException e) {}
	}
	
	/**
	 * Takes an array with that will be filled with next next bytes that get be received.
	 * @throws PLCAConnectionTimeoutException if the connection timed out
	 * @throws IOException if anything went wrong with the I/O
	 */
	public void readByte(byte[] toFill) throws IOException, PLCAConnectionTimeoutException {
		// Iterates over all slots
		for(int i=0;i<toFill.length;i++)
			// Fill the slot
			toFill[i] = this.readByte();
	}
	
	/**
	 * Reads a single unsigned byte
	 * @return the byte
	 * @throws IOException if anything went wrong with the I/O
	 * @throws PLCAConnectionTimeoutException
	 */
	public short readUByte() throws IOException,PLCAConnectionTimeoutException {
		// Updates the timeout
		this.lastSendTime = System.currentTimeMillis();
		
		// Waits until the timeout has been reached
		while(System.currentTimeMillis()-this.lastSendTime <= this.sendTimeout) {
			try {
				// Checks if a new byte got received
				if(this.reader.available() > 0)
					return (short) this.reader.read();
				else
					Thread.sleep(10);
			} catch (IOException e) {
				//TODO: Handle logging
				this.killConnection();
				throw e;
			} catch(InterruptedException e) {};
			
		}
		// Kills the connection
		this.killConnection();
		//TODO: handle logging
		throw new PLCAConnectionTimeoutException();
	}
	
	/**
	 * Reads a single signed byte
	 * @return the byte
	 * @throws IOException if anything went wrong with the I/O
	 * @throws PLCAConnectionTimeoutException if the connection timed out
	 */
	public byte readByte() throws IOException,PLCAConnectionTimeoutException {
		return (byte)(this.readUByte() & 0xff);
	}
	
	/**
	 * Sends data to the remote device
	 * @param data the byte to send
	 * @throws IOException if an i/o error occurs
	 */
	public void write(byte data) throws IOException {
		this.writer.write(data);
	}
	
	/**
	 * Writes all bytes to the stream
	 * @param data the data to send
	 * @throws IOException if anything went wrong with I/O
	 */
	public void write(byte[] data) throws IOException{
		this.writer.write(data);
	}
	
	// Wrapper to flush to the socket
	public void flush() throws IOException {
		this.writer.flush();
	}
	
	/**
	 * Kills the socket connection
	 */
	public void killConnection() {
		// Checks if the socket is still alive
		if(this.socket != null) {
			try {
				// Tries to kill the socket
				this.socket.close();
			} catch (IOException e) {}
			this.socket = null;
		}
	}
}

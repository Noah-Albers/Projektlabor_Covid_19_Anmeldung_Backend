package de.noahalbers.plca.backend.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nullable;

import de.noahalbers.plca.backend.PLCA;
import de.noahalbers.plca.backend.server.reqeusts.RequestHandler;
import de.noahalbers.plca.backend.server.reqeusts.handlers.TestRequest;
import de.noahalbers.plca.backend.socket.PLCAConnection;

public class PLCAServer extends Thread{

	// Reference to the main app
	private PLCA plca = PLCA.getInstance();
	
	// Server listener
	private ServerSocket listener;
	
	// Registers all request-handlers
	private Map<Integer/*Id*/,RequestHandler> handlers = new HashMap<Integer/*Id*/,RequestHandler>();
	{
		handlers.put(0, new TestRequest());
	}
	
	public PLCAServer() throws IOException {
		// TODO: Handle exceptions
		this.listener = new ServerSocket(Integer.parseInt(this.plca.getConfig().get("port")));
	}
	
	@Override
	public void interrupt() {
		// Stops the server
		try {
			this.listener.close();
		} catch (IOException e) {}
	}
	
	@Override
	public void run() {
		// Waits for connections
		while(!this.isInterrupted()) {
			try {
				new PLCAConnection(this.listener.accept(), System.out::println).start();
			} catch (Exception e) {
				//TODO: handle better
				e.printStackTrace();
			}
		}
	}
	
	@Nullable
	public RequestHandler getHandlerById(int id) {
		return this.handlers.getOrDefault(id, null);
	}
	
}

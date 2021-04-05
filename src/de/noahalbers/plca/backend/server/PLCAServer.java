package de.noahalbers.plca.backend.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import javax.annotation.Nullable;

import de.noahalbers.plca.backend.PLCA;
import de.noahalbers.plca.backend.server.reqeusts.RequestHandler;
import de.noahalbers.plca.backend.server.reqeusts.handlers.GetStatusRequest;
import de.noahalbers.plca.backend.server.reqeusts.handlers.GrabUsersRequest;
import de.noahalbers.plca.backend.server.reqeusts.handlers.LoginRFIDRequest;
import de.noahalbers.plca.backend.server.reqeusts.handlers.LoginRequest;
import de.noahalbers.plca.backend.server.reqeusts.handlers.LogoutRequest;
import de.noahalbers.plca.backend.server.reqeusts.handlers.RegisterUserRequest;
import de.noahalbers.plca.backend.server.socket.PLCAConnection;

public class PLCAServer extends Thread{

	// Reference to the main app
	private PLCA plca = PLCA.getInstance();
	
	// Server listener
	private ServerSocket listener;
	
	// Generator for the connection id
	private Random random = new Random();
	
	// Registers all request-handlers
	@SuppressWarnings("serial")
	private Map<Integer/*Id*/,RequestHandler> handlers = new HashMap<Integer/*Id*/,RequestHandler>()
	{{
		put(0,new GrabUsersRequest());
		put(1,new GetStatusRequest());
		put(2,new LoginRequest());
		put(3,new LogoutRequest());
		put(4,new RegisterUserRequest());
		put(5,new LoginRFIDRequest());
	}};
	
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
				// Generates a random connection id
				long cid = this.random.nextLong();
				
				// Creates the connection
				new PLCAConnection(cid,this.listener.accept(), this.plca.getLogger()::info).start();
			} catch (Exception e) {
				//TODO: handle better (Remove config long test stuff)
				e.printStackTrace();
			}
		}
	}
	
	@Nullable
	public RequestHandler getHandlerById(int id) {
		return this.handlers.getOrDefault(id, null);
	}
	
}

package de.noahalbers.plca.backend.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import de.noahalbers.plca.backend.util.Nullable;

import de.noahalbers.plca.backend.PLCA;
import de.noahalbers.plca.backend.logger.Logger;
import de.noahalbers.plca.backend.server.reqeusts.RequestHandler;
import de.noahalbers.plca.backend.server.reqeusts.handlers.GetStatusRequest;
import de.noahalbers.plca.backend.server.reqeusts.handlers.GrabUsersRequest;
import de.noahalbers.plca.backend.server.reqeusts.handlers.LoginRFIDRequest;
import de.noahalbers.plca.backend.server.reqeusts.handlers.LoginRequest;
import de.noahalbers.plca.backend.server.reqeusts.handlers.LogoutAllRequest;
import de.noahalbers.plca.backend.server.reqeusts.handlers.LogoutRequest;
import de.noahalbers.plca.backend.server.reqeusts.handlers.RegisterUserRequest;
import de.noahalbers.plca.backend.server.reqeusts.handlers.admin.AdminAuthcodeRequest;
import de.noahalbers.plca.backend.server.reqeusts.handlers.admin.AdminEditUserReqeust;
import de.noahalbers.plca.backend.server.reqeusts.handlers.admin.AdminFreezeSelfRequest;
import de.noahalbers.plca.backend.server.reqeusts.handlers.admin.AdminGetProfileRequest;
import de.noahalbers.plca.backend.server.reqeusts.handlers.admin.AdminGrabUserRequest;
import de.noahalbers.plca.backend.server.reqeusts.handlers.admin.AdminInfectedContactsReqeust;
import de.noahalbers.plca.backend.server.socket.PLCAConnection;

public class PLCAServer extends Thread{

	// Reference to the main app
	private PLCA plca = PLCA.getInstance();
	
	// Logger
	private Logger log = new Logger("PLCA-Server");
	
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
		put(6,new GetStatusRequest());
		put(7,new LogoutAllRequest());
		put(8,new AdminAuthcodeRequest());
		put(9,new AdminEditUserReqeust());
		put(10,new AdminFreezeSelfRequest());
		put(11,new AdminGetProfileRequest());
		put(12,new AdminGrabUserRequest());
		put(13,new AdminInfectedContactsReqeust());
	}};
	
	public PLCAServer() throws IOException {
		this.listener = new ServerSocket(this.plca.getConfig().getUnsafe("port"));
	}
	
	@Override
	public void interrupt() {
		this.log.debug("Stopping server (Interrupting Thread)");
		
		// Stops the server
		try {
			this.listener.close();
		} catch (IOException e) {}
	}
	
	@Override
	public void run() {
		
		this.log.debug("Starting server");
		
		// Waits for connections
		while(!this.isInterrupted()) {
			try {
				// Generates a random connection id
				long cid = this.random.nextLong();
				
				// Creates the connection
				new PLCAConnection(cid,this.listener.accept(), x->{}).start();
			} catch (Exception e) {
				this.log.error("Error with a connection").critical(e);
			}
		}
	}
	
	@Nullable
	public RequestHandler getHandlerById(int id) {
		return this.handlers.getOrDefault(id, null);
	}
	
}

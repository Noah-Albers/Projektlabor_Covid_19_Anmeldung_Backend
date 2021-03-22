package de.noahalbers.plca.backend.server.socket;

public enum ConnectionStatus {

	DISCONNECTED_SUCCESS,		// The request completed without any problems. Successfully disconnected the client afterwards.
	DISCONNECTED_IO, 			// If anything went wrong with the I/O
	DISCONNECTED_JSON, 			// The send message could not be parsed to json
	DISCONNECTED_AUTH_ERROR, 	// If anything went wrong with the handshake
	DISCONNECTED_NO_HANDLER, 	// If the request contains an invalid handler
	DISCONNECTED_PERMISSION; 	// If the requesting client doesn't have the permission to access the required resource
	
}

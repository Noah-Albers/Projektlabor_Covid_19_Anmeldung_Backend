package de.noahalbers.plca.backend.server.socket;

public enum ConnectionStatus {

	DISCONNECTED_SUCCESS,
	DISCONNECTED_IO, // If anything went wrong with the I/O
	DISCONNECTED_JSON, // The send message could not be parsed to json
	DISCONNECTED_AUTH_ERROR; // If anything went wrong with the handshake
	
}

package de.noahalbers.plca.backend.socket;

public enum ConnectionStatus {

	DISCONNECTED_SUCCESS,
	DISCONNECTED_AUTH_ERROR; // If anything went wrong with the handshake
	
}

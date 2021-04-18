package de.noahalbers.plca.backend.database.exceptions;

public class EntitySaveException extends Exception{
	private static final long serialVersionUID = -4285564826987341856L;

	// If a mistake has been made while setting put the code. Eg. attribute that do not exist should be saved.
	public final boolean codeError;
	
	// The name of the attribute that failed to load
	public final String key;
	
	public EntitySaveException(boolean codeError,String key) {
		this.codeError = codeError;
		this.key=key;
	}
}

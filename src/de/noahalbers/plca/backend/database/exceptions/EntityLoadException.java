package de.noahalbers.plca.backend.database.exceptions;

public class EntityLoadException extends Exception{
	private static final long serialVersionUID = -7946581282320220447L;

	public final boolean
	// If a required attribute is not present or failed to load (true) or if an optional attribute failed to load (false)
	requiredFailed,
	// If a mistake has been made while setting put the code. Eg. attribute that do not exist should be loaded.
	codeError;
	
	// The name of the attribute that failed to load
	public final String key;
	
	public EntityLoadException(boolean requiredFailed,boolean codeError,String key) {
		this.key=key;
		this.requiredFailed=requiredFailed;
		this.codeError=codeError;
	}
	
}

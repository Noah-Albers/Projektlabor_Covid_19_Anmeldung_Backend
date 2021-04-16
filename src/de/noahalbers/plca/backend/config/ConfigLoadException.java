package de.noahalbers.plca.backend.config;

public class ConfigLoadException extends Exception{
	private static final long serialVersionUID = 1L;
	private String attributeName;
	
	public ConfigLoadException(String attributeName) {
		this.attributeName=attributeName;
	}
	
	public String getAttributeName() {
		return attributeName;
	}
	
}

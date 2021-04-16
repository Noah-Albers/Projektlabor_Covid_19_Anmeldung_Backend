package de.noahalbers.plca.backend.config.loaders;

public abstract class BaseValue<T>{
	
	// The current value
	public T value;
	
	public BaseValue(T defaultValue) {
		this.value=defaultValue;
	}
	
	public abstract boolean loadObject(String value);
	
	public abstract String saveValue();

}

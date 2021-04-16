package de.noahalbers.plca.backend.config.loaders;

public class StringValue extends BaseValue<String>{

	public StringValue(String defaultValue) {
		super(defaultValue);
	}

	@Override
	public boolean loadObject(String value) {
		if(value.trim().isEmpty())
			return false;
		this.value=value;
		return true;
	}

	@Override
	public String saveValue() {
		return this.value;
	}

}

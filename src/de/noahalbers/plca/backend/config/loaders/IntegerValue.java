package de.noahalbers.plca.backend.config.loaders;

public class IntegerValue extends BaseValue<Integer>{

	public IntegerValue(Integer defaultValue) {
		super(defaultValue);
	}

	@Override
	public boolean loadObject(String value) {
		try {
			this.value = Integer.valueOf(value);
			return true;
		}catch(Exception e) {
			return false;
		}
	}

	@Override
	public String saveValue() {
		return this.value.toString();
	}

}

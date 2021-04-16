package de.noahalbers.plca.backend.config.loaders;

public class LongValue extends BaseValue<Long>{

	public LongValue(Long defaultValue) {
		super(defaultValue);
	}

	@Override
	public boolean loadObject(String value) {
		try {
			this.value = Long.valueOf(value);
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

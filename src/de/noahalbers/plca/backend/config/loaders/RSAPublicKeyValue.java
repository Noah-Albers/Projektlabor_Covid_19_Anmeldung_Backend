package de.noahalbers.plca.backend.config.loaders;

import java.security.spec.RSAPublicKeySpec;

import org.json.JSONObject;

import de.noahalbers.plca.backend.EncryptionManager;

public class RSAPublicKeyValue extends BaseValue<RSAPublicKeySpec> {

	public RSAPublicKeyValue(RSAPublicKeySpec defaultValue) {
		super(defaultValue);
	}

	@Override
	public boolean loadObject(String value) {
		try {
			// Tries to load the json object
			JSONObject o = new JSONObject(value);

			// Tries to load the values from json
			this.value = EncryptionManager.getPublicKeySpecFromJson(o);
			
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	@Override
	public String saveValue() {
		return EncryptionManager.getJsonFromPublicKeySpec(this.value).toString();
	}

	
}

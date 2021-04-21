package de.noahalbers.plca.backend.database.entitys;

import java.lang.reflect.Field;
import java.sql.Timestamp;
import java.util.Map;

import de.noahalbers.plca.backend.util.Nullable;

import de.noahalbers.plca.backend.database.EntityInfo;

public class AdminEntity extends Entity{

	public static final String
	ID = "id",
	NAME = "name",
	AUTH_CODE = "authcode",
	AUTH_CODE_EXPIRE = "authcodetimeout",
	IS_FROZEN = "isfrozen",
	EMAIL = "email",
	RSA_KEY = "clientrsapublic",
	PERMISSIONS = "permissions";

	// Copy-Paste generated. Just change the class name
	// Automatically grabs and stores all attributes from the class to easily serialize and deserialize those
	private static Map<String, Field> ATTRIBUTES = getAttributes(AdminEntity.class);
	public static final String[] ATTRIBUTE_LIST = getAttributeNames(AdminEntity.class);
	public static final String[] OPTIONAL_ATTRIBUTE_LIST = getAttributeNames(AdminEntity.class, true);
	public static final String[] REQUIRED_ATTRIBUTE_LIST = getAttributeNames(AdminEntity.class, false);
	
	@Override
	protected Map<String, Field> attributes() {
		return ATTRIBUTES;
	}
	
	public AdminEntity() {}
	
	// Id to identify the admin
	@EntityInfo(ID)
	public Integer id;
	
	// The name of the admin (Just in use for persons that view him)
	@EntityInfo(NAME)
	public String name;
	
	// Code that is used to identify if a request is valid (Weak 2fa)
	@EntityInfo(AUTH_CODE)
	@Nullable public Long authCode;
	
	// At what time the autocode expires
	@EntityInfo(AUTH_CODE_EXPIRE)
	@Nullable public Timestamp authCodeExpire;
	// If the account got frozen by an emergency message
	@EntityInfo(IS_FROZEN)
	public Boolean isFrozen;
	// What id the used has on telegram (Used to provide the weak 2fa auth)
	@EntityInfo(EMAIL)
	public String email;

	// The public key for the communication from the admin's client
	@EntityInfo(RSA_KEY)
	public String publicKey;

	// The permissions that the account has
	@EntityInfo(PERMISSIONS)
	public Integer permissions;
}

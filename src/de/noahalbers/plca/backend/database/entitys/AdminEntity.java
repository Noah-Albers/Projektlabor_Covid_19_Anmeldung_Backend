package de.noahalbers.plca.backend.database.entitys;

import java.lang.reflect.Field;
import java.sql.Timestamp;
import java.util.Map;

import javax.annotation.Nullable;

import de.noahalbers.plca.backend.database.EntityInfo;

public class AdminEntity extends Entity{

	public static final String
	ID = "id",
	NAME = "name",
	AUTH_CODE = "authcode",
	AUTH_CODE_EXPIRE = "authcodetimeout",
	IS_FROZEN = "isfrozen",
	TELEGRAM_CHAT_ID = "telegramchatid",
	RSA_KEY = "clientrsapublic",
	PERMISSIONS = "permissions";

	// Holds all entrys
	private static Map<String, Field> DB_ENTRYS = getEntrys(AdminEntity.class, true);
	private static Map<String, Field> JSON_ENTRYS = getEntrys(AdminEntity.class, false);
	
	// Holds a list with all database entrys. Can be used to load all values from a class
	public static final String[] DB_ENTRY_LIST = DB_ENTRYS.keySet().toArray(new String[DB_ENTRYS.size()]);
	
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
	@EntityInfo(TELEGRAM_CHAT_ID)
	public Long telegramChatId;

	// The public key for the communication from the admin's client
	@EntityInfo(RSA_KEY)
	public String publicKey;

	// The permissions that the account has
	@EntityInfo(PERMISSIONS)
	public Integer permissions;

	@Override
	protected Map<String, Field> entrys(boolean databaseEntrys) {
		return databaseEntrys?DB_ENTRYS:JSON_ENTRYS;
	}
}

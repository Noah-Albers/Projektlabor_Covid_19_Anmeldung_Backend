package de.noahalbers.plca.backend.database.entitys;

import java.security.spec.RSAPublicKeySpec;
import java.sql.Date;
import java.sql.ResultSet;

import javax.annotation.Nullable;

import org.json.JSONObject;

import de.noahalbers.plca.backend.EncryptionManager;

public class AdminEntity {

	private static final String DB_RSA_PUBLIC_KEY = "clientrsapublic",
						 	    DB_ID = "id",
						 	    DB_NAME = "name",
						 	    DB_AUTH_CODE = "authcode",
						 	    DB_AUTH_CODE_DATE = "authcodetimeout",
						 	    DB_FROZEN = "isfrozen",
						 	    DB_TELEGRAM_ID = "telegramchatid";
	
	// Id to identify the admin
	private final int id;
	
	// The name of the admin (Just in use for persons that view him)
	private String name;
	// Code that is used to identify if a request is valid (Weak 2fa)
	@Nullable
	private long authCode;
	// At what time the autocode expires
	private Date authCodeExpire;
	// If the account got frozen by an emergency message
	private boolean isFrozen;
	// What id the used has on telegram (Used to provide the weak 2fa auth)
	private long telegramChatId;
	// The public key for the communication from the admin's client
	private RSAPublicKeySpec publicKey;
	
	public AdminEntity(int id) {
		this.id=id;
	}

	public AdminEntity(int id, String name, int authCode, Date authCodeExpire, boolean isFrozen, long telegramChatId,
			RSAPublicKeySpec publicKey) {
		this.id = id;
		this.name = name;
		this.authCode = authCode;
		this.authCodeExpire = authCodeExpire;
		this.isFrozen = isFrozen;
		this.telegramChatId = telegramChatId;
		this.publicKey = publicKey;
	}

	public AdminEntity(ResultSet sqlResult) throws Exception {
		// Tries to load the public and private key
		this.publicKey = EncryptionManager.getPublicKeySpecFromJson(new JSONObject(sqlResult.getString(DB_RSA_PUBLIC_KEY)));
		
		// Sets all other fields
		this.id = sqlResult.getInt(DB_ID);
		this.name = sqlResult.getString(DB_NAME);
		this.authCode = sqlResult.getLong(DB_AUTH_CODE);
		this.authCodeExpire = sqlResult.getDate(DB_AUTH_CODE_DATE);
		this.isFrozen = sqlResult.getBoolean(DB_FROZEN);
		this.telegramChatId = sqlResult.getLong(DB_TELEGRAM_ID);
	}
	
	public String getName() {
		return this.name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public long getAuthCode() {
		return this.authCode;
	}
	public void setAuthCode(long authCode) {
		this.authCode = authCode;
	}
	public Date getAuthCodeExpire() {
		return this.authCodeExpire;
	}
	public void setAuthCodeExpire(Date authCodeExpire) {
		this.authCodeExpire = authCodeExpire;
	}
	public boolean isFrozen() {
		return this.isFrozen;
	}
	public void setFrozen(boolean isFrozen) {
		this.isFrozen = isFrozen;
	}
	public long getTelegramChatId() {
		return this.telegramChatId;
	}
	public void setTelegramChatId(long telegramChatId) {
		this.telegramChatId = telegramChatId;
	}
	public RSAPublicKeySpec getPublicKeySpec() {
		return this.publicKey;
	}
	public void setPublicKeySpec(RSAPublicKeySpec publicKey) {
		this.publicKey = publicKey;
	}
	public int getId() {
		return this.id;
	}
}

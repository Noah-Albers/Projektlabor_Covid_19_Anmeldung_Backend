package de.noahalbers.plca.backend.database.entitys;

import java.security.spec.RSAPublicKeySpec;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

import javax.annotation.Nullable;

import org.json.JSONException;
import org.json.JSONObject;

import com.mysql.cj.exceptions.RSAException;

import de.noahalbers.plca.backend.EncryptionManager;
import de.noahalbers.plca.backend.database.DBInfo;

public class AdminEntity {

	// Id to identify the admin
	@DBInfo("id")
	private int id;

	// The name of the admin (Just in use for persons that view him)
	@DBInfo("name")
	private String name;

	// Code that is used to identify if a request is valid (Weak 2fa)
	@Nullable
	@DBInfo("authcode")
	private long authCode;

	// At what time the autocode expires
	@DBInfo("authcodetimeout")
	private Timestamp authCodeExpire;

	// If the account got frozen by an emergency message
	@DBInfo("isfrozen")
	private boolean isFrozen;

	// What id the used has on telegram (Used to provide the weak 2fa auth)
	@DBInfo("telegramchatid")
	private long telegramChatId;

	// The public key for the communication from the admin's client
	private RSAPublicKeySpec publicKey;
	private static final String DB_RSA_PUBLIC_KEY = "clientrsapublic";

	// The permissions that the account has
	@DBInfo("permissions")
	private int permissions;

	public AdminEntity(int id, String name, int authCode, Timestamp authCodeExpire, boolean isFrozen,
			long telegramChatId, RSAPublicKeySpec publicKey, int permissions) {
		this.id = id;
		this.name = name;
		this.authCode = authCode;
		this.authCodeExpire = authCodeExpire;
		this.isFrozen = isFrozen;
		this.telegramChatId = telegramChatId;
		this.publicKey = publicKey;
		this.permissions = permissions;
	}

	/*
	 * Used when grabbing a new instance from the database. Missing values will be
	 * filled
	 */
	public AdminEntity(ResultSet result) throws RSAException, JSONException, SQLException {
		// Tries to load the public and private key
		this.publicKey = EncryptionManager.getPublicKeySpecFromJson(new JSONObject(result.getString(DB_RSA_PUBLIC_KEY)));
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

	public Timestamp getAuthCodeExpire() {
		return authCodeExpire;
	}

	public void setAuthCodeExpire(Timestamp authCodeExpire) {
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

	public int getPermissions() {
		return this.permissions;
	}

	public void setPermissions(int permissions) {
		this.permissions = permissions;
	}
}

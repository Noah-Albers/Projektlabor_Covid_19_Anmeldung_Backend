package de.noahalbers.plca.backend;

import java.math.BigInteger;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.spec.RSAPublicKeySpec;
import java.util.Base64;
import java.util.Optional;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.json.JSONObject;

import com.mysql.cj.exceptions.RSAException;

import de.noahalbers.plca.backend.logger.Logger;

public class EncryptionManager {

	private static final String JSON_RSA_MODULUS = "modulus",
						 	    JSON_RSA_EXPONENT = "exponent";

	// The logger
	private Logger log = new Logger("EncryptionManager");
	
	// RSA-Encryption system
	private Cipher rsaCipher;
	private KeyFactory rsaFactory;
	
	// Aes-Encryption system
	private Cipher aesCipher;
	
	// SHA-256 hash system
	private MessageDigest digestSha256;
	
	// MD-5 hash system
	private MessageDigest digestmd5;
	
	// Secure random number generator
	private SecureRandom random = new SecureRandom();
	
	/**
	 * Creates the instances for all encryption/hash methodes.
	 * @return empty if everything went right; if any encryption/hash method is not supported, return the name
	 */
	public Optional<String> init(){
		// Checks if aes-cbc with pkcs5 padding is supported
		if((this.aesCipher = this.getExceptNull(()->Cipher.getInstance("AES/CBC/PKCS5PADDING"))) == null)
			return Optional.of("aes");

		// Checks if rsa is supported
		if((this.rsaCipher = this.getExceptNull(()->Cipher.getInstance("RSA"))) == null)
			return Optional.of("rsa");

		// Checks if rsa is supported
		if((this.rsaFactory = this.getExceptNull(()->KeyFactory.getInstance("RSA"))) == null)
			return Optional.of("rsa factory");

		// Checks if sha256 is supported
		if((this.digestSha256 = this.getExceptNull(()->MessageDigest.getInstance("SHA-256"))) == null)
			return Optional.of("sha256");
		
		// Checks if sha256 is supported
		if((this.digestmd5 = this.getExceptNull(()->MessageDigest.getInstance("MD5"))) == null)
			return Optional.of("md5");
			
		return Optional.empty();
	}
	
	/**
	 * Encrypts the given blob of bytes using the public key
	 * @param data the data that shall be encrypted
	 * @param key the key to use
	 * @return empty if anything went wrong (error will be logged); otherwise the encrypted blob of data as bytes
	 */
	public Optional<byte[]> encryptRSA(byte[] data, PublicKey key) {
		return this.useRSA(data, key, Cipher.ENCRYPT_MODE);
	}
	
	/**
	 * Decrypts the given blob of bytes using the private key
	 * @param data the data that shall be decrypted
	 * @param key the key to use
	 * @return empty if anything went wrong (error will be logged); otherwise the decrypted blob of data as bytes
	 */
	public Optional<byte[]> decryptRSA(byte[] data, PrivateKey key){
		return this.useRSA(data, key, Cipher.DECRYPT_MODE);
	}
	
	/**
	 * Encrypts the given blob of bytes using the key and the iv
	 * @param data the data that shall be encrypted
	 * @param key the key to use
	 * @param iv the init vector for the encryption
	 * @return empty if anything went wrong (error will be logged); otherwise the encrypted blob of data as bytes
	 */
 	public Optional<byte[]> encryptAES(byte[] data, SecretKeySpec key, IvParameterSpec iv) {
		return this.useAES(data, key, iv, Cipher.ENCRYPT_MODE);
	}
	
	/**
	 * Decrypts the given blob of bytes using the key and the iv
	 * @param data the data that shall be decrypted
	 * @param key the key to use
	 * @param iv the init vector for the decryption
	 * @return empty if anything went wrong (error will be logged); otherwise the decrypted blob of data as bytes
	 */
	public Optional<byte[]> decryptAES(byte[] data, SecretKeySpec key, IvParameterSpec iv) {
		return this.useAES(data, key, iv, Cipher.DECRYPT_MODE);
	}
	
	/**
	 * Generates a random initialization vector for the aes-encryption (128 bit)
	 * @return
	 */
	public IvParameterSpec generateAESIV() {
		// Vector with all 128 bits
		byte[] vector = new byte[16];
		// Generates the init vector
		this.random.nextBytes(vector);
		// Returns the generated parameter spec
		return new IvParameterSpec(vector);
	}
	
	/**
	 * Generates a random aes key (256 bit).
	 * Depending on the {@link SecureRandom} implementation the method may block for a while
	 * @return the generated key
	 */
	public SecretKeySpec generateAESKey() {
		// Key with all 256 bits
		byte[] key = new byte[32];
		// Generates the random key
		this.random.nextBytes(key);
		
		// Generates the secret key
		return new SecretKeySpec(key, "AES");
	}

	/**
	 * Hashes the given bytes using md5
	 * @param data the raw data
	 * @return an hashed array with the md5 hash
	 */
	public byte[] hashMD5(byte[] data) {
		this.digestmd5.update(data);
		return this.digestmd5.digest();
	}
	
	/**
	 * Hashes the given bytes using sha256
	 * @param data the raw data
	 * @return an hashed array with the sha256 hash
	 */
	public byte[] hashSHA256(byte[] data) {
		this.digestSha256.update(data);
		return this.digestSha256.digest();
	}
	
	/**
	 * Loads a public key spec from an json object
	 * @param obj the json object that holds the public key
	 * @throws RSAException if anything went wrong during the loading
	 * @return the public key's spec
	 */
	public static RSAPublicKeySpec getPublicKeySpecFromJson(JSONObject obj) throws RSAException{
		try {
			// Tries to get the key from the result
			return new RSAPublicKeySpec(
				parseBase64ToBigint(obj.getString(JSON_RSA_MODULUS)),
				parseBase64ToBigint(obj.getString(JSON_RSA_EXPONENT))
			);
		}catch(Exception e) {
			throw new RSAException(e);
		}
	}
	
	/**
	 * Loads a json-object from the given public key spec.
	 * 
	 * @param spec the spec to gets as json
	 * @return an json object that contains the public key spec as base64 strings. This object can again be loaded by {@link #getPublicKeyFromSpec(RSAPublicKeySpec)}
	 */
	public static JSONObject getJsonFromPublicKeySpec(RSAPublicKeySpec spec) {
		return new JSONObject() {{
			this.put(JSON_RSA_MODULUS, parseBigintToBase64(spec.getModulus()));
			this.put(JSON_RSA_EXPONENT, parseBigintToBase64(spec.getPublicExponent()));
		}};
	}
	
	/**
	 * Converts a public key spec to a public key
	 * @param spec
	 * @return
	 * @throws RSAException
	 */
	public PublicKey getPublicKeyFromSpec(RSAPublicKeySpec spec) throws RSAException {
		try {
			// Tries to get the key from the result
			return this.rsaFactory.generatePublic(spec);
		}catch(Exception e) {
			throw new RSAException(e);
		}
	}
	
	/**
	 * Parses the base64 encoded string and converts it to a bitinteger
	 *  
	 * @param base64 the base64 encoded string
	 * @return the parsed bitint
	 * @throws IllegalArgumentException if the given text is not in base64
	 */
	private static BigInteger parseBase64ToBigint(String base64) throws IllegalArgumentException{
		return new BigInteger(1,Base64.getDecoder().decode(base64.trim()));
	}
	/**
	 * Parses the bigint to an base64-string
	 * 
	 * @param bigint the bitinteger to parse
	 * @return a string of base64-character that contains the bigint
	 */
	private static String parseBigintToBase64(BigInteger bigint) {
		return Base64.getEncoder().encodeToString(bigint.toByteArray());
	}
	
	/**
	 * Wrapper to use aes for en- and decryption
	 * @param data the data that shall be used
	 * @param key key for the algorithm
	 * @param iv the init vector for aes
	 * @param mode the mode that shall be used; eigther Cipher.ENCRYPT_MODE or Cipher.DECRYPT_MODE
	 * @return empty if anything went wrong (error will be logged); otherwise the modified blob of data
	 */
	private Optional<byte[]> useAES(byte[] data, SecretKeySpec key, IvParameterSpec iv, int mode){
		try {
			this.aesCipher.init(mode, key, iv);
			return Optional.of(this.aesCipher.doFinal(data));
		} catch (InvalidKeyException | InvalidAlgorithmParameterException | IllegalBlockSizeException | BadPaddingException e) {
			this.log.error("Failed to use aes").critical(e);
			return Optional.empty();
		}
	}

	/**
	 * Wrapper to use rsa for en- and decryption 
	 * @param data the data that shall be used
	 * @param key key for the algorithm (Public for encryption; Private for decryption)
	 * @param mode the mode that shall be used; eigther Cipher.ENCRYPT_MODE or Cipher.DECRYPT_MODE
	 * @return empty if anything went wrong (error will be logged); otherwise the modified blob of data
	 */
	private Optional<byte[]> useRSA(byte[] data, Key key, int mode){
		try {
			this.rsaCipher.init(mode, key);
			return Optional.of(this.rsaCipher.doFinal(data));
		} catch (InvalidKeyException | IllegalBlockSizeException | BadPaddingException e) {
			this.log.debug("Failed to use RSA").critical(e);
			return Optional.empty();
		}
	}
	
	/**
	 * Tries to get the value from the getter and return it; if an exception occures, return null
	 * @param getter
	 * @return
	 */
	private <T> T getExceptNull(UnsafeProvider<T> getter){
		try {
			return getter.get();
		} catch (Exception e) {
			return null;
		}
	}
	
	@FunctionalInterface
	public interface UnsafeProvider<T>{
		public T get() throws Exception;
	}
}

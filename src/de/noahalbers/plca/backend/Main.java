package de.noahalbers.plca.backend;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class Main {
	
	/*public static BigInteger d(String x) {
		return new BigInteger(1,Base64.getDecoder().decode(x.trim()));
	}/**/
	
	    private static final String cipherTransformation    = "AES/CBC/PKCS5PADDING";
	    private static final String aesEncryptionAlgorithem = "AES";
	
	    public static byte[] hash(byte[] x,String alg) throws Exception{
	    	MessageDigest md = MessageDigest.getInstance(alg);
	    	return md.digest(x);	    	
	    }
	    
	    public static byte[] hash(String x,String alg) throws Exception{
	    	return hash(x.getBytes(StandardCharsets.UTF_8),alg);
	    }
	    
	public static void main(String[] args) throws Exception {
		
		/*EncryptionManager m = new EncryptionManager();
		m.init();
		
		SecretKeySpec key = m.generateAESKey();
		
		IvParameterSpec iv = m.generateAESIV();
		
		String msg = "I am a little secret message lel";
		
		byte[] enc = m.encryptAES(msg.getBytes(StandardCharsets.UTF_8), key, iv).get();
				
		System.out.println("Encrypted: "+Base64.getEncoder().encodeToString(enc));
		System.out.println(Arrays.toString(enc));
		
		System.out.println();
		
		byte[] dec = m.decryptAES(enc, key, iv).get();
		
		System.out.println("Decrypted: "+new String(dec,StandardCharsets.UTF_8));
		
		System.out.println(Arrays.toString(dec));
		
		/*String pw = "ababababababababababababababababababababababababababababababab";
		String iv = "4r5t6z65tfgz7z6tfgzhefefef4r5t6z65tfgz7z6tfgzhefefef";
		
		String inp = "Testiger Täst";
		
		byte[] pwHash = hash(pw,"SHA-256");
		byte[] ivHash = hash(hash(iv,"SHA-256"),"MD5");
		
		System.out.println(Arrays.toString(ivHash));
		System.out.println(ivHash.length);
		
		String enc = encrypt(inp,ivHash,pwHash);
		
		String dec = decrypt(enc,ivHash,pwHash);
		
		System.out.println(enc);
		System.out.println(dec);/* */
		
		/*String inp = "AKhhe0VGt1J8BWZL8ca9BMhnXp9nqz0nQC6MtCdHz1+oKjre5GMdD+iMmIRqS8M8B2Bws+mxPFk0Ob5A/hU04yM5FUTfmfeM4Vh1ySiXD6pvwqeEtZYd5QrhmrbPGdd3r7HZPDd3sNVcqs0J0LAfpWDglVdhpAmhkj5wgZcWKJ8KUGeuZIas7hmwACALxHiA8e6YjD5glF1qGslALjd5wAoand5jJbB20QN9Ch8RFGjuhaOKELCkr2IC+r7uNb+eyrq85SMzHTKKrLaAIn7RsnRf2W03+1djW4QBQvnYLk55WNGX9idfRHq3KjrR0TMGzWbnhCBhDYKUQdok9jlhVQ==";
		
		String modules = "69cm48hB8zDLMlnFEsT1gb3X1Qce+LFZzR66ej+l4GbEWuAETJ7Y7Isnrj1V+VMWvv6QTDEzraegk5Qf5ghYqblKXyy5WsGcp/0PjBa6C2AsXdLyCj0A2hmTxH849RH9StbS6Bv+kv5yFIkfQxEoi0c2gyfJa5XqNmtILo2hQEl8NZDuDfpdaOMYgC732eBPQuQVjL3++36Yauvt7mkwhJMmIEA4J4so1NCZ1JqrddbzhvkH4f0R00qAZ2xdU9EmpBz2M2usBzFR6vGGQl7PgF8B7UdrScU+B0/IJTs4MJcUtD/uYtZv/dugBT5s43TSLRbfTYwDrBVj50NFJEAmpQ==";
		String d = "K29WpEDvlIB6C+3ok8pehOSX6Y4EMD1JkrOEGJu8SxXiN/JmGE5xGS+PwVFekiqxV7++h6nQIL1sn4F6Vn1XBvsvLVQpVwOtfGrpAB33abzuruD43tXZYFGxm1DrBoenpK7n7QOq4RunmwyeC1YPp4cYDRO4zu2vUxJzGs/O0O3dRPydUlYv33LR1jgPIhg091XZIAaVWuf0snzLEjqB9KRv4p/Lw6DCJ4T1HOxb+ClNr0do2rPBd4jxRL5WiV1qU157btVKsjdTVe9mBJzHK4EWN08UYprTOB8u07VlSY+uWg0CO70x7JAgqxnGOZhN7ACeRAGo3UAEipkbIztdSQ==";
		String exponent = "AQAB";
		String p = "8wb0eB/uEwDwSOlnm0OGi0RexCXNucdoTuXhHUbI4A4Zp9l38n7vmIq12Yebo736VT+NAI2mDiYyLR0+/7bu5CsFR7TAN+sdLbxNn+rRi5wwQErk2wBRIqzLC1fmNiuFA4Xb/+aEwZ9V2A9jRk6hCtLOq5p9ZAJmmofeJG7D91s=";
		String q = "+G386lm36hRpDGVnrXo9noFd5pfapk/YBjDksIWSkQr517dTx9r9mVx9qM2Ufx4Fz4PuVnBdtofXnnaZNeosM/LyH6QyBG4ozxrbeE5qMGo40M/flF2pbnmlS5vS+Mtm/gNu6YG36yk+MZnBKXqvkB3H4MyNeym6SWF7JiGkuf8=";
		String dp = "jwjM4daJHI403fThkbRQf68hx3goeOswLR+HOt/qYlRqLSwFOKgVBEa/eOQjal/p8jqRFn5H+ZPpfDv2VDKqr4fSd72eVEsAxWZKmZBu/ChVDcIGqA6rsijY6Dg3ujIq+0PRk+9suzi4IY4M89hbWC9yi/VFT5NKKDFHQVIf7Hk=";
		String dq = "ergqdcG4HkPS2LLHEmUOR1x0fFKLShirB/PjJGf3TPr1DGCdXF6LY9TSpi5Xqqu0BxyPAdgkEuTr2HWRnZZ+G3Nps8OyiYm5UwVbYQSrZBs00cG+GlZiOhaRVtY003329IfRpDvWFK6c8HeZedoHG9P6RswFH9AvG8yxvtZjvxU=";
		String inverseQ = "j9OoWEy1graJliy3crd64uRlHc00WxLxmasZDj/USq6HPdkZazl7Zv1ZAA/aMvPf4avfNUhh36aN71451YGCVlHTSasXX4Wn/rBEElt0NIQjAuN94ABPKXiyOsqYyfFbEwtEoIQps0vYt66DQHp08ho/eE8TX/onnj5FrSIOQ90=";
		
		
		
		KeyFactory factory = KeyFactory.getInstance("RSA");
		Cipher cipher = Cipher.getInstance("RSA");
		
		RSAPrivateKeySpec privSpec = new RSAPrivateCrtKeySpec(
			d(modules),
			d(exponent),
			d(d),
			d(p),
			d(q),
			d(dp),
			d(dq),
			d(inverseQ)
		);
		PrivateKey privKey = factory.generatePrivate(privSpec);
		cipher.init(Cipher.DECRYPT_MODE, privKey);
		byte[] decrypted = cipher.doFinal(Base64.getDecoder().decode(inp));
		System.out.println("decrypted: " + new String(decrypted));/**/
		
		// Config for the program
		/*Config cfg = new Config()
				.register("token", null)
				.register("botname", null)
				.register("db_host", "localhost")
				.register("db_port", "3306")
				.register("db_user", "root")
				.register("db_password", "")
				.register("db_databasename", "test")
				.register("connection_timeout", "5000")
				.register("applogin_pubK", "").loadConfig();
		
		EncryptionManager n = new EncryptionManager();
		n.init();
		
		PublicKey pk = n.getPublicKeyFromSpec(EncryptionManager.getPublicKeySpecFromJson(new JSONObject(cfg.get("applogin_pubK"))));
		
		// Generates the secret for the communication (AES-Key and AES-Init-Vector)
		SecretKeySpec key = n.generateAESKey();
		IvParameterSpec iv = n.generateAESIV();
		
		// Holds the combined bytes of the key and iv as a packet to send
		byte[] packet = new byte[32+16];
		
		// Stores the key and iv into the packet
		System.arraycopy(key.getEncoded(), 0, packet, 0, 32);
		System.arraycopy(iv.getIV(), 0, packet, 32, 16);
		
		// Encrypts the packet using the rsa-key
		byte[] optEnc = n.encryptRSA(packet, pk).get();
		
		System.out.println(Arrays.toString(optEnc));
		System.out.println(optEnc.length);/**/
		
		PLCA.getInstance().init();
	}/**/
	
	/**
     * Method for Encrypt Plain String Data
     * @param plainText
     * @return encryptedText
     */
    public static String encrypt(String plainText,byte[] iv,byte[] key) {
        String encryptedText = "";
        try {
            Cipher cipher   = Cipher.getInstance(cipherTransformation);
            SecretKeySpec secretKey = new SecretKeySpec(key, aesEncryptionAlgorithem);
            IvParameterSpec ivparameterspec = new IvParameterSpec(iv);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivparameterspec);
            byte[] cipherText = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            Base64.Encoder encoder = Base64.getEncoder();
            encryptedText = encoder.encodeToString(cipherText);

        } catch (Exception E) {
             System.err.println("Encrypt Exception : "+E.getMessage());
        }
        return encryptedText;
    }

    /**
     * Method For Get encryptedText and Decrypted provided String
     * @param encryptedText
     * @return decryptedText
     */
    public static String decrypt(String encryptedText,byte[] iv,byte[] key) {
        String decryptedText = "";
        try {
            Cipher cipher = Cipher.getInstance(cipherTransformation);
            SecretKeySpec secretKey = new SecretKeySpec(key, aesEncryptionAlgorithem);
            IvParameterSpec ivparameterspec = new IvParameterSpec(iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, ivparameterspec);
            Base64.Decoder decoder = Base64.getDecoder();
            byte[] cipherText = decoder.decode(encryptedText.getBytes(StandardCharsets.UTF_8));
            decryptedText = new String(cipher.doFinal(cipherText), StandardCharsets.UTF_8);

        } catch (Exception E) {
            System.err.println("decrypt Exception : "+E.getMessage());
        }
        return decryptedText;
    }
	
//	public static void main(String[] args) throws Exception {
//		PLCA.getInstance().init();
//	}
}

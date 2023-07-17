package com.CredentialCoder;

import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;

public class Coder {

	private static final String Key = "FFNura782EsasDnKUEhJ4FhV";

	public static String Encode(String Input) throws NoSuchAlgorithmException, NoSuchPaddingException,
			InvalidKeyException, Exception, BadPaddingException {

		Key aeskey = new SecretKeySpec(Key.getBytes(), "AES");
		Cipher cipher = Cipher.getInstance("AES");
		cipher.init(Cipher.ENCRYPT_MODE, aeskey);
		byte[] encrypted = cipher.doFinal(Input.getBytes());
		Base64.Encoder encoder = Base64.getEncoder();
		String encodingString = encoder.encodeToString(encrypted);
		return "AES:" + encodingString;

	}

	public static String decode(String Input) throws NoSuchAlgorithmException, NoSuchPaddingException,
			InvalidKeyException, Exception, BadPaddingException {

		String[] parts = Input.split(":");
		if (parts.length == 2) {
			Key aesKey = new SecretKeySpec(Key.getBytes(), "AES");
			Cipher cipher = Cipher.getInstance("AES");
			Base64.Decoder decoder = Base64.getDecoder();
			cipher.init(Cipher.DECRYPT_MODE, aesKey);
			String decryptedString = new String(cipher.doFinal(decoder.decode(parts[parts.length - 1])));
			return decryptedString;
		} else {

			return "unable to Decrypt" + Input;
		}

	}
	
	public static void main(String[] args) throws InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException, BadPaddingException, Exception {
		System.out.println(Coder.decode("AES:s+Z/a55EmCfIzeb+lqd1GkgjlN/U1ueW8d+tJ+A/wIP8PBRQk405qLZksNhoD5tl"));
		System.out.println(Coder.decode("AES:YiJe10c7B36A9kpNBgb03w=="));
		
		System.out.println(Coder.Encode("shaik.jakeerhussain217@outlook.com"));
		System.out.println(Coder.Encode("Jakeer787#"));
		
	}

}

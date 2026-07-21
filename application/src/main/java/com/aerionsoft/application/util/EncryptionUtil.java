package com.aerionsoft.application.util;
import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

import com.aerionsoft.application.exception.ServiceExceptions;
public class EncryptionUtil {

    private static final String AES_ALGO = "AES/CBC/PKCS5Padding";
    private static final String HMAC_ALGO = "HmacSHA256";
    private static final String KEY_DERIVATION_ALGO = "PBKDF2WithHmacSHA256";
    private static final int KEY_SIZE_BITS = 256;
    private static final int ITERATIONS = 65536;
    private static final int IV_LENGTH = 16; // For CBC mode
    private static final int HMAC_LENGTH = 32; // 256 bits
    private static final String SALT = "01987c6c-ec87-782a-b3f9-9212b1a370f3"; // store securely!

    // Encrypts and returns: Base64(HMAC + IV + Ciphertext)
    public static String encrypt(String plainText) {
        try {
            byte[] iv = new byte[IV_LENGTH];
            SecureRandom random = new SecureRandom();
            random.nextBytes(iv);

            SecretKeySpec aesKey = deriveKey("StrongPassword@2025", SALT, "AES");
            SecretKeySpec hmacKey = deriveKey("StrongPassword@2025", SALT, "HMAC");

            Cipher cipher = Cipher.getInstance(AES_ALGO);
            cipher.init(Cipher.ENCRYPT_MODE, aesKey, new IvParameterSpec(iv));
            byte[] ciphertext = cipher.doFinal(plainText.getBytes());

            // Combine IV + ciphertext
            byte[] ivAndCiphertext = new byte[iv.length + ciphertext.length];
            System.arraycopy(iv, 0, ivAndCiphertext, 0, iv.length);
            System.arraycopy(ciphertext, 0, ivAndCiphertext, iv.length, ciphertext.length);

            // Calculate HMAC
            Mac hmac = Mac.getInstance(HMAC_ALGO);
            hmac.init(hmacKey);
            byte[] hmacBytes = hmac.doFinal(ivAndCiphertext);

            // Final payload: HMAC + IV + Ciphertext
            byte[] finalPayload = new byte[hmacBytes.length + ivAndCiphertext.length];
            System.arraycopy(hmacBytes, 0, finalPayload, 0, hmacBytes.length);
            System.arraycopy(ivAndCiphertext, 0, finalPayload, hmacBytes.length, ivAndCiphertext.length);

            return Base64.getEncoder().encodeToString(finalPayload);

        } catch (Exception e) {
            throw ServiceExceptions.internal("Encryption failed", e);
        }
    }

    // Decrypts after verifying HMAC
    public static String decrypt(String base64Input) {
        try {
            byte[] input = Base64.getDecoder().decode(base64Input);
            byte[] hmacReceived = Arrays.copyOfRange(input, 0, HMAC_LENGTH);
            byte[] ivAndCiphertext = Arrays.copyOfRange(input, HMAC_LENGTH, input.length);

            SecretKeySpec aesKey = deriveKey("StrongPassword@2025", SALT, "AES");
            SecretKeySpec hmacKey = deriveKey("StrongPassword@2025", SALT, "HMAC");

            // Recompute HMAC
            Mac hmac = Mac.getInstance(HMAC_ALGO);
            hmac.init(hmacKey);
            byte[] hmacExpected = hmac.doFinal(ivAndCiphertext);

            // Constant-time comparison
            if (!MessageDigest.isEqual(hmacReceived, hmacExpected)) {
                throw new SecurityException("Invalid HMAC: Data integrity compromised");
            }

            // Extract IV and Ciphertext
            byte[] iv = Arrays.copyOfRange(ivAndCiphertext, 0, IV_LENGTH);
            byte[] ciphertext = Arrays.copyOfRange(ivAndCiphertext, IV_LENGTH, ivAndCiphertext.length);

            Cipher cipher = Cipher.getInstance(AES_ALGO);
            cipher.init(Cipher.DECRYPT_MODE, aesKey, new IvParameterSpec(iv));
            byte[] decrypted = cipher.doFinal(ciphertext);
            return new String(decrypted);

        } catch (SecurityException se) {
            throw se;
        } catch (Exception e) {
            throw ServiceExceptions.internal("Decryption failed", e);
        }
    }

    // Derives a 256-bit key from password and salt for AES or HMAC
    private static SecretKeySpec deriveKey(String password, String salt, String type) throws Exception {
        PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt.getBytes(), ITERATIONS, KEY_SIZE_BITS);
        SecretKeyFactory factory = SecretKeyFactory.getInstance(KEY_DERIVATION_ALGO);
        byte[] keyBytes = factory.generateSecret(spec).getEncoded();
        return new SecretKeySpec(keyBytes, type.equals("HMAC") ? HMAC_ALGO : "AES");
    }

    // 🔍 Testing
    public static void main(String[] args) {
        String password = "StrongPassword@2025";
//        String plainText = "96948c33-778e-435a-815c-c592bb27b29d";
//
        String encrypted = encrypt("0f92de18-b425-4eed-a86c-f7ddc362b791");
        System.out.println("Encrypted: " + encrypted);
//
//        String decrypted = decrypt("3o5RSwgD5eT1c1dg14kgO6iQfmWKNQd2zT9DiVrDMg9Nz77oDmGO5yKUj/+uImKcI9FkKsHun66z3Uj5fvNpLhRO+mUaG86R9FXip/7bMe8Zlmv8BMbtHfCN0VOzLEan");
//        System.out.println("Decrypted: " + decrypted);


//        String decrypted2 = decrypt("r8x1VsxSRKAK64hKUsea5P2WMFpSkJVtdbR3x2LL+rWqDIRYUnVBTfj/hAGqeP7FAOeJapycb8CVe7ZzxN3dAYUJh4DhSr+x9Ll1intWE8WRz9TpGixCeHrbuLHO7Jtn");
//        System.out.println("Decrypted: " + decrypted2);
    }
}

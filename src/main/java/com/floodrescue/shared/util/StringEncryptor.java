package com.floodrescue.shared.util;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Base64;

/**
 * Deterministic AES encryption for searchable fields (email, phone).
 * Uses AES/CBC with a key-derived IV so same plaintext always produces same ciphertext,
 * enabling JPA derived queries (findByPhone, findByEmail, etc.) to work correctly.
 */
public class StringEncryptor {

    private static final String ALGORITHM = "AES/CBC/PKCS5Padding";
    private final SecretKeySpec keySpec;
    private final IvParameterSpec ivSpec;

    public StringEncryptor(String secretKey) {
        try {
            byte[] keyHash = MessageDigest.getInstance("SHA-256")
                    .digest(secretKey.getBytes(StandardCharsets.UTF_8));
            this.keySpec = new SecretKeySpec(Arrays.copyOf(keyHash, 16), "AES");

            byte[] ivHash = MessageDigest.getInstance("MD5")
                    .digest(secretKey.getBytes(StandardCharsets.UTF_8));
            this.ivSpec = new IvParameterSpec(ivHash);
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize StringEncryptor", e);
        }
    }

    public String encrypt(String plainText) {
        if (plainText == null) return null;
        try {
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec);
            byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            throw new RuntimeException("Encryption failed", e);
        }
    }

    public String decrypt(String encryptedText) {
        if (encryptedText == null) return null;
        try {
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);
            byte[] decoded = Base64.getDecoder().decode(encryptedText);
            byte[] decrypted = cipher.doFinal(decoded);
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Decryption failed", e);
        }
    }
}

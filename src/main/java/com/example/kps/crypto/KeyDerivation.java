package com.example.kps.crypto;

import com.example.kps.server.KeyMaterial;
import com.example.kps.server.crypto.IntMath;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;

/**
 * KeyDerivation handles:
 *   1. Computing the shared scalar between two users.
 *   2. Hashing that scalar into a shared AES key.
 *   3. AES encryption/decryption helper functions.
 */
public final class KeyDerivation {

    // Prevent instantiation (utility class)
    private KeyDerivation() {}

    /**
     * Computes the shared key K(A, B) for two users A and B using:
     *
     *   K = ( sum_i ( secretShareA[i] * publicVectorB[i] ) mod p )
     *
     * This produces a shared scalar.
     * That scalar is then hashed into a 128-bit AES key.
     *
     * @param self               The client's private key material.
     * @param otherUserId        ID of the other user.
     * @param otherPublicVector  Public vector of the other user.
     * @return AES-128 key (16 bytes)
     */
    public static byte[] computeSharedKey(KeyMaterial self, String otherUserId, int[] otherPublicVector) {

        // Make sure both public vectors have same length (dimension check)
        if (self.getPublicVector().length != otherPublicVector.length) {
            throw new IllegalArgumentException("Vector dimension mismatch");
        }

        long acc = 0;                     // Accumulator for the dot-product
        int prime = self.getPrime();      // Shared prime-modulus for system
        int[] secret = self.getSecretShare(); // User’s private vector

        // Compute sum(secret[i] * otherPublic[i]) mod prime
        for (int i = 0; i < secret.length; i++) {
            long product = ((long) secret[i] * otherPublicVector[i]) % prime;
            acc = (acc + product) % prime;
        }

        // Convert to an int inside correct modulus
        int sharedScalar = IntMath.mod((int) acc, prime);

        // Hash scalar + user IDs → derive shared symmetric key
        byte[] keyMaterial = hashSharedScalar(self.getUserId(), otherUserId, sharedScalar);

        // Return 16 bytes = AES-128 key
        return Arrays.copyOf(keyMaterial, 16);
    }

    /**
     * Hashes:
     *    - the shared scalar
     *    - the two user IDs (in sorted order)
     *
     * This ensures:
     *    Hash(A,B) == Hash(B,A)
     *
     * We use SHA-256, and then the first 16 bytes become the AES key.
     */
    private static byte[] hashSharedScalar(String selfId, String otherId, int scalar) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            // Convert scalar to bytes
            ByteBuffer buffer = ByteBuffer.allocate(Integer.BYTES);
            buffer.putInt(scalar);

            // Ensure ordering: ID1:ID2 always in lexicographic order
            // This prevents A→B and B→A from generating different keys
            String orderedIds = (selfId.compareTo(otherId) <= 0)
                    ? selfId + ":" + otherId
                    : otherId + ":" + selfId;

            // Feed scalar bytes and ordered IDs into SHA-256
            digest.update(buffer.array());
            digest.update(orderedIds.getBytes(StandardCharsets.UTF_8));

            return digest.digest();  // 32-byte SHA-256 hash
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Missing SHA-256", e);
        }
    }

    /**
     * Encrypts plaintext using AES-128 in CBC mode with PKCS#5 padding.
     *
     * Format of returned bytes: [IV || CIPHERTEXT]
     */
    public static byte[] encryptAes(byte[] key, byte[] plaintext) {
        try {
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");

            // Generate a random 16-byte IV (AES block size)
            byte[] iv = new byte[16];
            new SecureRandom().nextBytes(iv);

            // Initialize cipher for encryption
            cipher.init(
                    Cipher.ENCRYPT_MODE,
                    new SecretKeySpec(key, "AES"),
                    new IvParameterSpec(iv)
            );

            // Encrypt message
            byte[] ciphertext = cipher.doFinal(plaintext);

            // Prepend IV to ciphertext (sender must send IV)
            byte[] withIv = new byte[iv.length + ciphertext.length];
            System.arraycopy(iv, 0, withIv, 0, iv.length);
            System.arraycopy(ciphertext, 0, withIv, iv.length, ciphertext.length);

            return withIv;

        } catch (Exception e) {
            throw new IllegalStateException("AES encryption failed", e);
        }
    }

    /**
     * Decrypts AES/CBC ciphertext.
     *
     * Input format: [IV || CIPHERTEXT]
     */
    public static byte[] decryptAes(byte[] key, byte[] ivAndCiphertext) {
        try {
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");

            // Extract IV (first 16 bytes)
            byte[] iv = Arrays.copyOfRange(ivAndCiphertext, 0, 16);

            // Extract ciphertext
            byte[] ciphertext = Arrays.copyOfRange(ivAndCiphertext, 16, ivAndCiphertext.length);

            // Initialize AES decryptor
            cipher.init(
                    Cipher.DECRYPT_MODE,
                    new SecretKeySpec(key, "AES"),
                    new IvParameterSpec(iv)
            );

            // Return decrypted plaintext
            return cipher.doFinal(ciphertext);

        } catch (Exception e) {
            throw new IllegalStateException("AES decryption failed", e);
        }
    }

    /**
     * Utility for converting byte arrays to hex strings.
     * Useful for debugging keys and ciphertext.
     */
    public static String toHex(byte[] data) {
        StringBuilder sb = new StringBuilder();
        for (byte b : data) {
            sb.append(String.format("%02x", b)); // convert 1 byte → 2 hex chars
        }
        return sb.toString();
    }
}
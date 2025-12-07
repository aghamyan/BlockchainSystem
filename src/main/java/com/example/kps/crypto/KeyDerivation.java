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
 * Client-side key derivation: computes the shared scalar and derives an AES key.
 */
public final class KeyDerivation {
    private KeyDerivation() {}

    /**
     * Compute K(A,B) = (A_i * S) * A_j^T mod p using own secret share and other's public vector.
     */
    public static byte[] computeSharedKey(KeyMaterial self, String otherUserId, int[] otherPublicVector) {
        if (self.getPublicVector().length != otherPublicVector.length) {
            throw new IllegalArgumentException("Vector dimension mismatch");
        }
        long acc = 0;
        int prime = self.getPrime();
        int[] secret = self.getSecretShare();
        for (int i = 0; i < secret.length; i++) {
            long product = ((long) secret[i] * otherPublicVector[i]) % prime;
            acc = (acc + product) % prime;
        }
        int sharedScalar = IntMath.mod((int) acc, prime);
        byte[] keyMaterial = hashSharedScalar(self.getUserId(), otherUserId, sharedScalar);
        return Arrays.copyOf(keyMaterial, 16); // AES-128 key
    }

    private static byte[] hashSharedScalar(String selfId, String otherId, int scalar) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            ByteBuffer buffer = ByteBuffer.allocate(Integer.BYTES);
            buffer.putInt(scalar);
            String orderedIds = (selfId.compareTo(otherId) <= 0)
                    ? selfId + ":" + otherId
                    : otherId + ":" + selfId;
            digest.update(buffer.array());
            digest.update(orderedIds.getBytes(StandardCharsets.UTF_8));
            return digest.digest();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Missing SHA-256", e);
        }
    }

    public static byte[] encryptAes(byte[] key, byte[] plaintext) {
        try {
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            byte[] iv = new byte[16];
            new SecureRandom().nextBytes(iv);
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"), new IvParameterSpec(iv));
            byte[] ciphertext = cipher.doFinal(plaintext);
            byte[] withIv = new byte[iv.length + ciphertext.length];
            System.arraycopy(iv, 0, withIv, 0, iv.length);
            System.arraycopy(ciphertext, 0, withIv, iv.length, ciphertext.length);
            return withIv;
        } catch (Exception e) {
            throw new IllegalStateException("AES encryption failed", e);
        }
    }

    public static byte[] decryptAes(byte[] key, byte[] ivAndCiphertext) {
        try {
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            byte[] iv = Arrays.copyOfRange(ivAndCiphertext, 0, 16);
            byte[] ciphertext = Arrays.copyOfRange(ivAndCiphertext, 16, ivAndCiphertext.length);
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"), new IvParameterSpec(iv));
            return cipher.doFinal(ciphertext);
        } catch (Exception e) {
            throw new IllegalStateException("AES decryption failed", e);
        }
    }

    public static String toHex(byte[] data) {
        StringBuilder sb = new StringBuilder();
        for (byte b : data) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}

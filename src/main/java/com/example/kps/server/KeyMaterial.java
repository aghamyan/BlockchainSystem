package com.example.kps.server;

import java.util.Arrays;
import java.util.Base64;
import java.util.Objects;

/**
 * Represents the predistributed key material given to a user in a Blom-like KPS system.
 *
 * For each user, the server provides:
 *   - userId        → the user identity
 *   - publicVector  → derived deterministically from userId
 *   - secretShare   → computed as  M × publicVector   (where M is the secret matrix)
 *   - prime         → modulus p used in all operations
 *
 * This KeyMaterial is all the client needs to independently compute shared keys with others.
 */
public class KeyMaterial {

    // User identity string (e.g., "userAlice")
    private final String userId;

    // Public vector for this user (SHA-256–derived from userId)
    private final int[] publicVector;

    // Secret share vector assigned to this user (M × publicVector)
    private final int[] secretShare;

    // Prime modulus used in all vector/matrix arithmetic
    private final int prime;

    /**
     * Construct a user's key material.
     */
    public KeyMaterial(String userId, int[] publicVector, int[] secretShare, int prime) {
        this.userId = userId;
        this.publicVector = publicVector;
        this.secretShare = secretShare;
        this.prime = prime;
    }

    // ---------------------------
    // Getters
    // ---------------------------

    public String getUserId() {
        return userId;
    }

    public int[] getPublicVector() {
        return publicVector;
    }

    public int[] getSecretShare() {
        return secretShare;
    }

    public int getPrime() {
        return prime;
    }

    // ---------------------------
    // Serialization / Deserialization
    // ---------------------------

    /**
     * Convert the KeyMaterial into a compact string format so it can be
     * transferred to the client during provisioning.
     *
     * Format:
     *   userId : prime : Base64(publicVectorBytes) : Base64(secretShareBytes)
     */
    public String serialize() {
        return userId + ":" + prime + ":" +
                encode(publicVector) + ":" + encode(secretShare);
    }

    /**
     * Reconstruct a KeyMaterial object from its serialized text form.
     */
    public static KeyMaterial deserialize(String encoded) {
        String[] parts = encoded.split(":");
        if (parts.length != 4) {
            throw new IllegalArgumentException("Invalid encoding");
        }

        String id = parts[0];
        int prime = Integer.parseInt(parts[1]);
        int[] pub = decode(parts[2]);     // decode Base64 → int[]
        int[] share = decode(parts[3]);   // decode Base64 → int[]

        return new KeyMaterial(id, pub, share, prime);
    }

    /**
     * Encodes an int[] vector into Base64.
     * Each int is stored using 4 bytes (big-endian).
     */
    private static String encode(int[] vector) {
        byte[] bytes = new byte[vector.length * 4];

        for (int i = 0; i < vector.length; i++) {
            int value = vector[i];
            int base = i * 4;

            // Convert int → 4 bytes big-endian
            bytes[base]     = (byte) (value >>> 24);
            bytes[base + 1] = (byte) (value >>> 16);
            bytes[base + 2] = (byte) (value >>> 8);
            bytes[base + 3] = (byte) value;
        }

        return Base64.getEncoder().encodeToString(bytes);
    }

    /**
     * Decodes Base64-encoded bytes back into an int[] vector.
     * Must be divisible by 4 bytes since each int uses 4 bytes.
     */
    private static int[] decode(String encoded) {
        byte[] bytes = Base64.getDecoder().decode(encoded);

        if (bytes.length % 4 != 0) {
            throw new IllegalArgumentException("Invalid vector encoding length");
        }

        int[] vector = new int[bytes.length / 4];

        for (int i = 0; i < vector.length; i++) {
            int base = i * 4;

            // Reconstruct 4 bytes → int
            int v =
                ((bytes[base]     & 0xFF) << 24) |
                ((bytes[base + 1] & 0xFF) << 16) |
                ((bytes[base + 2] & 0xFF) << 8)  |
                (bytes[base + 3]  & 0xFF);

            vector[i] = v;
        }

        return vector;
    }

    // ---------------------------
    // Object helpers
    // ---------------------------

    @Override
    public String toString() {
        return "KeyMaterial{" +
                "userId='" + userId + '\'' +
                ", publicVector=" + Arrays.toString(publicVector) +
                ", secretShare=" + Arrays.toString(secretShare) +
                ", prime=" + prime +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof KeyMaterial)) return false;

        KeyMaterial that = (KeyMaterial) o;

        return prime == that.prime &&
                Objects.equals(userId, that.userId) &&
                Arrays.equals(publicVector, that.publicVector) &&
                Arrays.equals(secretShare, that.secretShare);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(userId, prime);
        result = 31 * result + Arrays.hashCode(publicVector);
        result = 31 * result + Arrays.hashCode(secretShare);
        return result;
    }
}
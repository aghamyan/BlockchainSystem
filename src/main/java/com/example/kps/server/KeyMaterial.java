package com.example.kps.server;

import java.util.Arrays;
import java.util.Base64;
import java.util.Objects;

/**
 * Represents predistributed key material for a user.
 * Contains the public vector (derived from the user identity) and the secret share (A_i * S).
 */
public class KeyMaterial {
    private final String userId;
    private final int[] publicVector;
    private final int[] secretShare;
    private final int prime;

    public KeyMaterial(String userId, int[] publicVector, int[] secretShare, int prime) {
        this.userId = userId;
        this.publicVector = publicVector;
        this.secretShare = secretShare;
        this.prime = prime;
    }

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

    /**
     * Serialize the material in a simple text format for provisioning.
     */
    public String serialize() {
        return userId + ":" + prime + ":" + encode(publicVector) + ":" + encode(secretShare);
    }

    public static KeyMaterial deserialize(String encoded) {
        String[] parts = encoded.split(":");
        if (parts.length != 4) {
            throw new IllegalArgumentException("Invalid encoding");
        }
        String id = parts[0];
        int prime = Integer.parseInt(parts[1]);
        int[] pub = decode(parts[2]);
        int[] share = decode(parts[3]);
        return new KeyMaterial(id, pub, share, prime);
    }

    private static String encode(int[] vector) {
        byte[] bytes = new byte[vector.length * 4];
        for (int i = 0; i < vector.length; i++) {
            int v = vector[i];
            int base = i * 4;
            bytes[base] = (byte) (v >>> 24);
            bytes[base + 1] = (byte) (v >>> 16);
            bytes[base + 2] = (byte) (v >>> 8);
            bytes[base + 3] = (byte) v;
        }
        return Base64.getEncoder().encodeToString(bytes);
    }

    private static int[] decode(String encoded) {
        byte[] bytes = Base64.getDecoder().decode(encoded);
        if (bytes.length % 4 != 0) {
            throw new IllegalArgumentException("Invalid vector encoding length");
        }
        int[] vector = new int[bytes.length / 4];
        for (int i = 0; i < vector.length; i++) {
            int base = i * 4;
            int v = ((bytes[base] & 0xFF) << 24)
                    | ((bytes[base + 1] & 0xFF) << 16)
                    | ((bytes[base + 2] & 0xFF) << 8)
                    | (bytes[base + 3] & 0xFF);
            vector[i] = v;
        }
        return vector;
    }

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
        return prime == that.prime && Objects.equals(userId, that.userId)
                && Arrays.equals(publicVector, that.publicVector)
                && Arrays.equals(secretShare, that.secretShare);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(userId, prime);
        result = 31 * result + Arrays.hashCode(publicVector);
        result = 31 * result + Arrays.hashCode(secretShare);
        return result;
    }
}

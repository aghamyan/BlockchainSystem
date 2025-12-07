package com.example.kps.client;

import com.example.kps.crypto.KeyDerivation;
import com.example.kps.server.KeyMaterial;

import java.util.Map;
import java.util.Objects;

/**
 * Client representation holding predistributed key material.
 */
public class UserClient {
    private final KeyMaterial keyMaterial;
    private final Map<String, int[]> publicDirectory;

    public UserClient(KeyMaterial keyMaterial, Map<String, int[]> publicDirectory) {
        this.keyMaterial = Objects.requireNonNull(keyMaterial, "keyMaterial");
        this.publicDirectory = Objects.requireNonNull(publicDirectory, "publicDirectory");
    }

    public byte[] computeSharedKey(String otherUserId) {
        int[] otherVector = publicDirectory.get(otherUserId);
        if (otherVector == null) {
            throw new IllegalArgumentException("Unknown user: " + otherUserId);
        }
        return KeyDerivation.computeSharedKey(keyMaterial, otherUserId, otherVector);
    }

    public KeyMaterial getKeyMaterial() {
        return keyMaterial;
    }
}

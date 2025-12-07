package com.example.kps.client;

import com.example.kps.crypto.KeyDerivation;
import com.example.kps.server.KeyMaterial;

import java.util.Map;
import java.util.Objects;

/**
 * Represents a client/user in a Key Predistribution System (KPS).
 * Each client holds:
 *   - Some private key material (given ahead of time)
 *   - A public directory containing public vectors for all users
 *
 * Using these two pieces, the client can compute a shared secret key
 * with any other user by applying the key derivation function.
 */
public class UserClient {

    // The private key material assigned to this user.
    // Only this user has access to this material.
    private final KeyMaterial keyMaterial;

    // A map from userId → that user's public vector.
    // This is publicly known information.
    private final Map<String, int[]> publicDirectory;

    /**
     * Creates a new UserClient.
     *
     * @param keyMaterial    The private key material assigned to this user.
     * @param publicDirectory A public map containing public vectors for all users.
     */
    public UserClient(KeyMaterial keyMaterial, Map<String, int[]> publicDirectory) {

        // Objects.requireNonNull(...) ensures null values throw an immediate error
        this.keyMaterial = Objects.requireNonNull(keyMaterial, "keyMaterial");
        this.publicDirectory = Objects.requireNonNull(publicDirectory, "publicDirectory");
    }

    /**
     * Computes a shared key between this user and another user.
     *
     * @param otherUserId The ID of the user we want to communicate with.
     * @return A byte[] representing the shared secret key.
     */
    public byte[] computeSharedKey(String otherUserId) {

        // Look up the other user's public vector from the directory
        int[] otherVector = publicDirectory.get(otherUserId);

        // If the user does not exist in the directory, stop
        if (otherVector == null) {
            throw new IllegalArgumentException("Unknown user: " + otherUserId);
        }

        // Use KeyDerivation to compute the shared secret
        return KeyDerivation.computeSharedKey(
                keyMaterial, // this user's private info
                otherUserId, // ID of the other user
                otherVector  // the other user's public vector
        );
    }

    /**
     * Returns this client's private key material (read-only).
     */
    public KeyMaterial getKeyMaterial() {
        return keyMaterial;
    }
}
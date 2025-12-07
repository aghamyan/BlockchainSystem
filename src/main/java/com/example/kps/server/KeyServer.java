package com.example.kps.server;

import com.example.kps.server.crypto.VectorUtils;

import java.security.SecureRandom;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * KeyServer acts as the central authority in a Key Predistribution System.
 *
 * Responsibilities:
 *   - Generates global parameters: prime modulus and dimension
 *   - Creates a secret symmetric matrix S (never revealed to clients)
 *   - Registers users by giving them:
 *        * publicVector  = derived from userId (SHA-based)
 *        * secretShare   = publicVector × S  (mod prime)
 *
 * Clients later use these values to independently compute shared keys:
 *    K(A,B) = secretShareA · publicVectorB  (mod prime)
 *
 * This ensures both sides compute the same shared key WITHOUT server involvement.
 */
public class KeyServer {

    // Global modulus p used for all modular arithmetic
    private final int prime;

    // Dimension of vectors and the secret matrix
    private final int dimension;

    // Secret symmetric matrix S (server-only, NEVER sent to clients)
    private int[][] secretMatrix;

    // Stores all registered users and their KeyMaterial
    private final Map<String, KeyMaterial> users = new HashMap<>();

    // Secure random generator for matrix generation
    private final SecureRandom random = new SecureRandom();

    /**
     * Creates the key server and initializes a fresh secret matrix.
     */
    public KeyServer(int prime, int dimension) {
        this.prime = prime;
        this.dimension = dimension;
        rotateMasterMatrix(); // generates new symmetric matrix S
    }

    /**
     * Registers a new user by:
     *   1. Deriving public vector A_i from userId via SHA-256
     *   2. Computing secret share = A_i × S (mod p)
     *   3. Returning KeyMaterial that client will use for shared key generation
     */
    public KeyMaterial registerUser(String userId) {

        // Deterministic public vector from userId
        int[] publicVector = VectorUtils.derivePublicVector(userId, dimension, prime);

        // Compute secret share using the symmetric matrix S
        int[] secretShare = VectorUtils.multiply(publicVector, secretMatrix, prime);

        // Package it into KeyMaterial
        KeyMaterial material = new KeyMaterial(userId, publicVector, secretShare, prime);

        // Store in internal registry
        users.put(userId, material);

        return material;
    }

    /**
     * Rotates the server’s secret matrix S.
     * This generates a brand-new matrix and removes all existing users,
     * since their secret shares become invalid.
     *
     * In a real-world system:
     *   - all users would require fresh provisioning
     *   - this is used for long-term security renewal
     */
    public void rotateMasterMatrix() {
        secretMatrix = VectorUtils.randomSymmetricMatrix(dimension, prime, random);
        users.clear(); // old shares no longer valid
    }

    /**
     * Retrieves a user's KeyMaterial if they are registered.
     */
    public Optional<KeyMaterial> getUser(String userId) {
        return Optional.ofNullable(users.get(userId));
    }

    /**
     * Returns the public directory: userId → publicVector.
     *
     * This directory is SAFE to share with everyone.
     * Clients use it to compute shared keys.
     */
    public Map<String, int[]> getPublicDirectory() {
        Map<String, int[]> directory = new HashMap<>();

        users.forEach((id, material) ->
                directory.put(id, material.getPublicVector())
        );

        return Collections.unmodifiableMap(directory);
    }

    // Getters for system parameters
    public int getPrime() {
        return prime;
    }

    public int getDimension() {
        return dimension;
    }

    /**
     * Returns the secret matrix (for demo/debugging only).
     * In a real system, this would NEVER be exposed.
     */
    public int[][] getSecretMatrix() {
        return secretMatrix;
    }
}
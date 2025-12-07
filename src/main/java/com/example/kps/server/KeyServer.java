package com.example.kps.server;

import com.example.kps.server.crypto.VectorUtils;

import java.security.SecureRandom;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Central authority that generates global parameters and predistributes key material to users.
 * Implements a Blom-like symmetric matrix scheme: K_ij = A_i * S * A_j^T (mod p).
 */
public class KeyServer {
    private final int prime;
    private final int dimension;
    private int[][] secretMatrix;
    private final Map<String, KeyMaterial> users = new HashMap<>();
    private final SecureRandom random = new SecureRandom();

    public KeyServer(int prime, int dimension) {
        this.prime = prime;
        this.dimension = dimension;
        rotateMasterMatrix();
    }

    /**
     * Registers a user, deriving a deterministic public vector and computing the secret share A_i * S.
     */
    public KeyMaterial registerUser(String userId) {
        int[] publicVector = VectorUtils.derivePublicVector(userId, dimension, prime);
        int[] secretShare = VectorUtils.multiply(publicVector, secretMatrix, prime);
        KeyMaterial material = new KeyMaterial(userId, publicVector, secretShare, prime);
        users.put(userId, material);
        return material;
    }

    /**
     * Recomputes the symmetric master matrix and clears existing user shares.
     * In a real system, clients would need fresh provisioning after rotation.
     */
    public void rotateMasterMatrix() {
        secretMatrix = VectorUtils.randomSymmetricMatrix(dimension, prime, random);
        users.clear();
    }

    public Optional<KeyMaterial> getUser(String userId) {
        return Optional.ofNullable(users.get(userId));
    }

    public Map<String, int[]> getPublicDirectory() {
        Map<String, int[]> directory = new HashMap<>();
        users.forEach((id, material) -> directory.put(id, material.getPublicVector()));
        return Collections.unmodifiableMap(directory);
    }

    public int getPrime() {
        return prime;
    }

    public int getDimension() {
        return dimension;
    }

    public int[][] getSecretMatrix() {
        return secretMatrix;
    }
}

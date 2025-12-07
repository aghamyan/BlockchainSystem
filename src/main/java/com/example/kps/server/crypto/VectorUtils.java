package com.example.kps.server.crypto;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;

/**
 * Utility class for:
 *   - deriving public vectors from user IDs
 *   - multiplying vectors and matrices (mod prime)
 *   - generating a random symmetric matrix
 *   - formatting matrices for printing
 *
 * These operations form the core of a Blom-like key predistribution scheme.
 */
public final class VectorUtils {

    // Utility class → prevent instantiation
    private VectorUtils() {}

    /**
     * Derives a deterministic public vector for a given userId using SHA-256.
     * Each user gets a unique public vector computed from their ID.
     *
     * @param userId    the unique user identifier
     * @param dimension the length of the vector
     * @param prime     the modulus
     * @return int[] public vector for this user
     */
    public static int[] derivePublicVector(String userId, int dimension, int prime) {
        try {
            // Compute SHA-256 hash of userId
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(userId.getBytes(StandardCharsets.UTF_8));

            int[] vector = new int[dimension];

            // Derive integers from successive 4-byte chunks of the hash
            for (int i = 0; i < dimension; i++) {
                int chunk =
                        ((hash[4 * i] & 0xFF) << 24) |
                        ((hash[4 * i + 1] & 0xFF) << 16) |
                        ((hash[4 * i + 2] & 0xFF) << 8) |
                        (hash[4 * i + 3] & 0xFF);

                // Reduce to modulo prime to keep values in valid range
                vector[i] = IntMath.mod(chunk, prime);
            }

            return vector;

        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    /**
     * Multiplies a vector by a matrix (vector × matrix) under modulo arithmetic.
     *
     * Example:
     *   result[j] = Σ_i ( vector[i] * matrix[i][j] ) mod prime
     *
     * This is used to compute each user's secret share = M × publicVector.
     */
    public static int[] multiply(int[] vector, int[][] matrix, int prime) {

        int[] result = new int[vector.length];

        // For each column j of the matrix
        for (int j = 0; j < matrix[0].length; j++) {
            long acc = 0;

            // Perform dot-product: sum(vector[i] * matrix[i][j])
            for (int i = 0; i < vector.length; i++) {
                long product = ((long) vector[i] * matrix[i][j]) % prime;
                acc = (acc + product) % prime;
            }

            // Ensure result is positive modulo p
            result[j] = IntMath.mod((int) acc, prime);
        }

        return result;
    }

    /**
     * Generates a random symmetric matrix (M = Mᵀ).
     * The symmetry is required for Blom’s scheme so that:
     *     K(A,B) = K(B,A)
     *
     * @param dimension matrix size (dimension × dimension)
     * @param prime     modulus
     * @param random    secure random generator
     * @return symmetric matrix
     */
    public static int[][] randomSymmetricMatrix(int dimension, int prime, SecureRandom random) {

        int[][] matrix = new int[dimension][dimension];

        // Fill the upper triangle then mirror to lower triangle
        for (int i = 0; i < dimension; i++) {
            for (int j = i; j < dimension; j++) {

                // Avoid tiny values (0 or 1) for better security margin
                int value = random.nextInt(prime - 2) + 2;

                matrix[i][j] = value;
                matrix[j][i] = value;  // enforce symmetry
            }
        }

        return matrix;
    }

    /**
     * Formats a matrix into a readable multi-line string.
     * Useful for printing the server’s secret matrix in demonstrations.
     */
    public static String formatMatrix(int[][] matrix) {
        StringBuilder sb = new StringBuilder();

        for (int[] row : matrix) {
            sb.append(Arrays.toString(row))
              .append(System.lineSeparator());
        }

        return sb.toString();
    }
}
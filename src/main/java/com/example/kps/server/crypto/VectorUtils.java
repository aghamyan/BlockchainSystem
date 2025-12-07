package com.example.kps.server.crypto;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;

/**
 * Vector and matrix operations for the Blom-like scheme.
 */
public final class VectorUtils {
    private VectorUtils() {}

    public static int[] derivePublicVector(String userId, int dimension, int prime) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(userId.getBytes(StandardCharsets.UTF_8));
            int[] vector = new int[dimension];
            for (int i = 0; i < dimension; i++) {
                int chunk = ((hash[4 * i] & 0xFF) << 24)
                        | ((hash[4 * i + 1] & 0xFF) << 16)
                        | ((hash[4 * i + 2] & 0xFF) << 8)
                        | (hash[4 * i + 3] & 0xFF);
                vector[i] = IntMath.mod(chunk, prime);
            }
            return vector;
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    public static int[] multiply(int[] vector, int[][] matrix, int prime) {
        int[] result = new int[vector.length];
        for (int j = 0; j < matrix[0].length; j++) {
            long acc = 0;
            for (int i = 0; i < vector.length; i++) {
                long product = ((long) vector[i] * matrix[i][j]) % prime;
                acc = (acc + product) % prime;
            }
            result[j] = IntMath.mod((int) acc, prime);
        }
        return result;
    }

    public static int[][] randomSymmetricMatrix(int dimension, int prime, SecureRandom random) {
        int[][] matrix = new int[dimension][dimension];
        for (int i = 0; i < dimension; i++) {
            for (int j = i; j < dimension; j++) {
                int value = random.nextInt(prime - 2) + 2; // avoid tiny values
                matrix[i][j] = value;
                matrix[j][i] = value;
            }
        }
        return matrix;
    }

    public static String formatMatrix(int[][] matrix) {
        StringBuilder sb = new StringBuilder();
        for (int[] row : matrix) {
            sb.append(Arrays.toString(row)).append(System.lineSeparator());
        }
        return sb.toString();
    }
}

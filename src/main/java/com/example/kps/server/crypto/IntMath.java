package com.example.kps.server.crypto;

/**
 * A tiny helper class for modular arithmetic.
 * Ensures that values are always reduced correctly modulo a prime.
 */
public final class IntMath {

    // Private constructor → this is a pure utility class (no objects allowed)
    private IntMath() {}

    /**
     * Computes (value mod prime) but also ensures the result is **always positive**.
     *
     * In Java, (-3 % 5) = -3   <-- negative result
     * But in modular math, (-3 mod 5) = 2.
     *
     * This method fixes that issue:
     *  - If result >= 0, return it normally.
     *  - If result < 0, add `prime` to make it positive.
     *
     * @param value The number to be reduced.
     * @param prime The modulus (must be positive).
     * @return The positive modulo value in the range [0, prime-1].
     */
    public static int mod(int value, int prime) {
        int v = value % prime;           // Java's raw remainder
        return v >= 0 ? v : v + prime;   // Ensure positive result
    }
}
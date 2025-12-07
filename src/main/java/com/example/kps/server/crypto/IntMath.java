package com.example.kps.server.crypto;

/**
 * Small helper for modular arithmetic.
 */
public final class IntMath {
    private IntMath() {}

    public static int mod(int value, int prime) {
        int v = value % prime;
        return v >= 0 ? v : v + prime;
    }
}

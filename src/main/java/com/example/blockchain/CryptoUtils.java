package com.example.blockchain;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * A utility class with cryptography helper functions.
 * It handles:
 *   - RSA key generation
 *   - SHA-256 hashing
 *   - Signing data with a private key
 *   - Verifying signatures with a public key
 *   - Encoding/decoding public keys for storage or transmission
 */
public final class CryptoUtils {

    // Private constructor = no one can create an instance of this utility class.
    private CryptoUtils() {}

    /**
     * Generates a new RSA key pair (public + private keys).
     */
    public static KeyPair generateKeyPair() {
        try {
            // Create an RSA key-pair generator
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");

            // Use 2048-bit RSA (secure enough for demonstration)
            generator.initialize(2048);

            // Return the generated key pair
            return generator.generateKeyPair();

        } catch (Exception e) {
            throw new IllegalStateException("Unable to generate RSA key pair", e);
        }
    }

    /**
     * Computes the SHA-256 hash of a string.
     * SHA-256 always outputs a 64-character hex string.
     */
    public static String sha256(String payload) {
        try {
            // Get a SHA-256 hasher
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            // Hash the input (convert string to bytes first)
            byte[] hash = digest.digest(payload.getBytes(StandardCharsets.UTF_8));

            // Convert hash bytes to a readable hex string
            StringBuilder builder = new StringBuilder();
            for (byte b : hash) {
                builder.append(String.format("%02x", b));
            }

            return builder.toString();

        } catch (Exception e) {
            throw new IllegalStateException("Unable to compute SHA-256", e);
        }
    }

    /**
     * Signs a message (payload) using a private RSA key.
     * Output is Base64 so it can be stored/transmitted easily.
     */
    public static String sign(String payload, PrivateKey privateKey) {
        try {
            // Use SHA256withRSA signature algorithm
            Signature signature = Signature.getInstance("SHA256withRSA");

            // Sign with private key
            signature.initSign(privateKey);

            // Load the message into the signature object
            signature.update(payload.getBytes(StandardCharsets.UTF_8));

            // Generate the signature bytes and convert to Base64
            return Base64.getEncoder().encodeToString(signature.sign());

        } catch (Exception e) {
            throw new IllegalStateException("Unable to sign payload", e);
        }
    }

    /**
     * Verifies a signature using the sender's public RSA key.
     * Returns true only if:
     *   - The signature is valid
     *   - The payload matches exactly what was signed
     */
    public static boolean verify(String payload, String signatureText, PublicKey publicKey) {
        try {
            // Prepare verification using same algorithm
            Signature signature = Signature.getInstance("SHA256withRSA");

            // Initialize verifier with sender's public key
            signature.initVerify(publicKey);

            // Provide the original message bytes
            signature.update(payload.getBytes(StandardCharsets.UTF_8));

            // Decode signature from Base64 back to bytes
            byte[] signatureBytes = Base64.getDecoder().decode(signatureText);

            // Verify it
            return signature.verify(signatureBytes);

        } catch (Exception e) {
            // If anything goes wrong, treat it as invalid
            return false;
        }
    }

    /**
     * Converts a public key to a Base64 string.
     * Useful for writing it to files or sending over the network.
     */
    public static String encodePublicKey(PublicKey key) {
        return Base64.getEncoder().encodeToString(key.getEncoded());
    }

    /**
     * Converts a Base64-encoded string back into a PublicKey object.
     */
    public static PublicKey decodePublicKey(String encodedKey) {
        try {
            // Decode Base64 into raw key bytes
            byte[] keyBytes = Base64.getDecoder().decode(encodedKey);

            // Use X.509 format (standard for public keys)
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(keyBytes);

            // Rebuild a PublicKey object for RSA
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            return keyFactory.generatePublic(keySpec);

        } catch (Exception e) {
            throw new IllegalArgumentException("Unable to decode public key", e);
        }
    }
}
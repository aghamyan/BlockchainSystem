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
 * Helper methods for generating keys, hashing payloads, and signing/verifying content.
 */
public final class CryptoUtils {
    private CryptoUtils() {}

    public static KeyPair generateKeyPair() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            return generator.generateKeyPair();
        } catch (Exception e) {
            throw new IllegalStateException("Unable to generate RSA key pair", e);
        }
    }

    public static String sha256(String payload) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(payload.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder();
            for (byte b : hash) {
                builder.append(String.format("%02x", b));
            }
            return builder.toString();
        } catch (Exception e) {
            throw new IllegalStateException("Unable to compute SHA-256", e);
        }
    }

    public static String sign(String payload, PrivateKey privateKey) {
        try {
            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initSign(privateKey);
            signature.update(payload.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(signature.sign());
        } catch (Exception e) {
            throw new IllegalStateException("Unable to sign payload", e);
        }
    }

    public static boolean verify(String payload, String signatureText, PublicKey publicKey) {
        try {
            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initVerify(publicKey);
            signature.update(payload.getBytes(StandardCharsets.UTF_8));
            byte[] signatureBytes = Base64.getDecoder().decode(signatureText);
            return signature.verify(signatureBytes);
        } catch (Exception e) {
            return false;
        }
    }

    public static String encodePublicKey(PublicKey key) {
        return Base64.getEncoder().encodeToString(key.getEncoded());
    }

    public static PublicKey decodePublicKey(String encodedKey) {
        try {
            byte[] keyBytes = Base64.getDecoder().decode(encodedKey);
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(keyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            return keyFactory.generatePublic(keySpec);
        } catch (Exception e) {
            throw new IllegalArgumentException("Unable to decode public key", e);
        }
    }
}

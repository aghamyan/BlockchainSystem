package com.example.kps.demo;

import com.example.kps.client.UserClient;
import com.example.kps.crypto.KeyDerivation;
import com.example.kps.server.KeyMaterial;
import com.example.kps.server.KeyServer;
import com.example.kps.server.ProvisioningService;
import com.example.kps.server.crypto.VectorUtils;

import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Demonstrates how a Key Predistribution System (KPS) works:
 *
 *  1. Server generates master secret and public vectors.
 *  2. Server provisions users with private key material.
 *  3. Users independently compute the SAME shared key without talking to server.
 *  4. Users use that key to encrypt/decrypt messages.
 */
public class DemoMain {
    public static void main(String[] args) {

        // -----------------------------
        // 1. Server setup parameters
        // -----------------------------
        int prime = 2147483647; // a large safe prime (≈ 2^31 - 1)
        int dimension = 4;      // vector dimension for demo (small for readability)

        // Create the central KPS server (holds the secret symmetric matrix)
        KeyServer server = new KeyServer(prime, dimension);

        // Service that provides key material to users
        ProvisioningService provisioning = new ProvisioningService(server);


        // -----------------------------
        // 2. Provision 3 users with keys
        // -----------------------------
        // Each user gets:
        //   - unique private vector (secret share)
        //   - public vector stored in the directory
        Map<String, String> packets =
                provisioning.provisionUsers("userAlice", "userBob", "userCharlie");

        // Deserialize key materials from provisioning packets
        KeyMaterial aliceMaterial = KeyMaterial.deserialize(packets.get("userAlice"));
        KeyMaterial bobMaterial   = KeyMaterial.deserialize(packets.get("userBob"));


        // -----------------------------
        // 3. Create client objects
        // -----------------------------
        // Each user has:
        //   - their private key material
        //   - server's public directory (all public vectors)
        UserClient alice = new UserClient(aliceMaterial, server.getPublicDirectory());
        UserClient bob   = new UserClient(bobMaterial, server.getPublicDirectory());


        // -----------------------------
        // 4. Users compute shared keys independently
        // -----------------------------
        // Alice computes K(Alice, Bob)
        byte[] aliceKey = alice.computeSharedKey("userBob");

        // Bob computes K(Bob, Alice)
        byte[] bobKey = bob.computeSharedKey("userAlice");

        // These keys MUST be identical
        System.out.println("Alice derived key: " + KeyDerivation.toHex(aliceKey));
        System.out.println("Bob derived key  : " + KeyDerivation.toHex(bobKey));
        System.out.println("Keys match? " + java.util.Arrays.equals(aliceKey, bobKey));


        // -----------------------------
        // 5. Example: Alice encrypts message to Bob
        // -----------------------------
        byte[] plaintext = "Hello Bob".getBytes(StandardCharsets.UTF_8);

        // Encrypt using shared AES key
        byte[] ciphertext = KeyDerivation.encryptAes(aliceKey, plaintext);

        // Bob decrypts using his derived key (must be the same)
        byte[] decrypted = KeyDerivation.decryptAes(bobKey, ciphertext);

        System.out.println("Ciphertext (hex): " + KeyDerivation.toHex(ciphertext));
        System.out.println("Decrypted text  : " +
                new String(decrypted, StandardCharsets.UTF_8));


        // -----------------------------
        // 6. Display public directory
        // -----------------------------
        // This directory contains only public vectors
        System.out.println("Public directory (vectors):");
        server.getPublicDirectory().forEach((id, vector) ->
                System.out.println(id + " -> " + java.util.Arrays.toString(vector)));


        // -----------------------------
        // 7. Display secret matrix (server only)
        // -----------------------------
        // Clients NEVER see this.
        // This is the master secret used to generate user shares.
        System.out.println("Secret symmetric matrix on server (hidden from clients):\n" +
                VectorUtils.formatMatrix(server.getSecretMatrix()));
    }
}
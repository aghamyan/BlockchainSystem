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
 * Demonstrates key predistribution, independent key agreement, and AES encryption.
 */
public class DemoMain {
    public static void main(String[] args) {
        int prime = 2147483647; // large prime close to 2^31
        int dimension = 4;     // small dimension for demo readability

        KeyServer server = new KeyServer(prime, dimension);
        ProvisioningService provisioning = new ProvisioningService(server);

        Map<String, String> packets = provisioning.provisionUsers("userAlice", "userBob", "userCharlie");

        KeyMaterial aliceMaterial = KeyMaterial.deserialize(packets.get("userAlice"));
        KeyMaterial bobMaterial = KeyMaterial.deserialize(packets.get("userBob"));

        UserClient alice = new UserClient(aliceMaterial, server.getPublicDirectory());
        UserClient bob = new UserClient(bobMaterial, server.getPublicDirectory());

        byte[] aliceKey = alice.computeSharedKey("userBob");
        byte[] bobKey = bob.computeSharedKey("userAlice");

        System.out.println("Alice derived key: " + KeyDerivation.toHex(aliceKey));
        System.out.println("Bob derived key  : " + KeyDerivation.toHex(bobKey));
        System.out.println("Keys match? " + java.util.Arrays.equals(aliceKey, bobKey));

        byte[] plaintext = "Hello Bob".getBytes(StandardCharsets.UTF_8);
        byte[] ciphertext = KeyDerivation.encryptAes(aliceKey, plaintext);
        byte[] decrypted = KeyDerivation.decryptAes(bobKey, ciphertext);

        System.out.println("Ciphertext (hex): " + KeyDerivation.toHex(ciphertext));
        System.out.println("Decrypted text  : " + new String(decrypted, StandardCharsets.UTF_8));

        System.out.println("Public directory (vectors):");
        server.getPublicDirectory().forEach((id, vector) ->
                System.out.println(id + " -> " + java.util.Arrays.toString(vector)));

        System.out.println("Secret symmetric matrix on server (hidden from clients):\n" +
                VectorUtils.formatMatrix(server.getSecretMatrix()));
    }
}

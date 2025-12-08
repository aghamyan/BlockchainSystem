package com.example;

import com.example.blockchain.Client;
import com.example.blockchain.FullNode;
import com.example.blockchain.Transaction;
import com.example.kps.client.UserClient;
import com.example.kps.crypto.KeyDerivation;
import com.example.kps.server.KeyMaterial;
import com.example.kps.server.KeyServer;
import com.example.kps.server.ProvisioningService;
import com.example.kps.server.crypto.VectorUtils;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Map;
import java.util.Scanner;

/**
 * Simple text-based UI that lets users run either the blockchain or KPS demo.
 */
public class AppUI {

    private final Scanner scanner = new Scanner(System.in);

    public static void main(String[] args) {
        new AppUI().run();
    }

    private void run() {
        System.out.println("====================================");
        System.out.println("  Interactive Cryptography Playground");
        System.out.println("====================================\n");

        boolean running = true;
        while (running) {
            System.out.println("Select a demo to run:");
            System.out.println("  1) Mini Blockchain network");
            System.out.println("  2) Key Predistribution System (KPS)");
            System.out.println("  0) Exit");

            String choice = prompt("Enter choice", "1");
            switch (choice) {
                case "1":
                    runBlockchainFlow();
                    break;
                case "2":
                    runKpsFlow();
                    break;
                case "0":
                    running = false;
                    break;
                default:
                    System.out.println("Unknown option. Please choose 1, 2, or 0.\n");
            }
        }

        System.out.println("Goodbye!");
    }

    private void runBlockchainFlow() {
        System.out.println("\n--- Mini Blockchain Demo ---");
        String sender = prompt("Sender name", "Alice");
        String recipient = prompt("Recipient name", "Bob");
        double amount = promptDouble("Amount to send", 10.50);
        int difficulty = promptInt("Mining difficulty (leading zeros)", 4);

        FullNode nodeA = new FullNode("full-node-A", difficulty);
        FullNode nodeB = new FullNode("full-node-B", difficulty);
        FullNode nodeC = new FullNode("full-node-C", difficulty);

        nodeA.connectPeer(nodeB);
        nodeB.connectPeer(nodeC);

        Client client = new Client(sender);
        Transaction tx = client.createSignedTransaction(recipient, amount);

        Path txFile = Path.of(sender.toLowerCase() + "_to_" + recipient.toLowerCase() + ".txn");
        client.writeTransactionToFile(tx, txFile);
        System.out.println("Transaction file written to " + txFile.toAbsolutePath());

        client.submitTransaction(nodeA, tx);
        System.out.println("Submitted transaction to nodeA. Mining pending transactions...\n");

        nodeB.minePendingTransactions();

        System.out.println("Ledger heights after mining:");
        System.out.println(nodeA.getNodeId() + " blocks: " + nodeA.getChainSnapshot().size());
        System.out.println(nodeB.getNodeId() + " blocks: " + nodeB.getChainSnapshot().size());
        System.out.println(nodeC.getNodeId() + " blocks: " + nodeC.getChainSnapshot().size());

        System.out.println("\nBlock details (from nodeA):");
        nodeA.getChainSnapshot().forEach(block -> {
            System.out.println("------------------------------");
            System.out.println("Index: " + block.getIndex());
            System.out.println("Hash:  " + block.getHash());
            System.out.println("Prev:  " + block.getPreviousHash());
            System.out.println("Tx count: " + block.getTransactions().size());
            block.getTransactions().forEach(t ->
                    System.out.println("  - " + t.getSender() + " -> " + t.getRecipient() +
                            " : " + t.getAmount() + " (id " + t.getId() + ")"));
        });

        System.out.println("\nBlockchain demo complete!\n");
    }

    private void runKpsFlow() {
        System.out.println("\n--- Key Predistribution System Demo ---");
        String userA = prompt("First user ID", "userAlice");
        String userB = prompt("Second user ID", "userBob");
        String message = prompt("Message to encrypt", "Hello from " + userA);

        int prime = 2147483647;
        int dimension = 4;
        KeyServer server = new KeyServer(prime, dimension);
        ProvisioningService provisioning = new ProvisioningService(server);

        Map<String, String> packets = provisioning.provisionUsers(userA, userB);
        KeyMaterial materialA = KeyMaterial.deserialize(packets.get(userA));
        KeyMaterial materialB = KeyMaterial.deserialize(packets.get(userB));

        UserClient clientA = new UserClient(materialA, server.getPublicDirectory());
        UserClient clientB = new UserClient(materialB, server.getPublicDirectory());

        byte[] keyA = clientA.computeSharedKey(userB);
        byte[] keyB = clientB.computeSharedKey(userA);

        System.out.println("Derived keys (should match):");
        System.out.println(userA + " key: " + KeyDerivation.toHex(keyA));
        System.out.println(userB + " key: " + KeyDerivation.toHex(keyB));
        System.out.println("Keys equal? " + Arrays.equals(keyA, keyB));

        byte[] ciphertext = KeyDerivation.encryptAes(keyA, message.getBytes(StandardCharsets.UTF_8));
        byte[] plaintext = KeyDerivation.decryptAes(keyB, ciphertext);

        System.out.println("Ciphertext (hex): " + KeyDerivation.toHex(ciphertext));
        System.out.println("Decrypted text  : " + new String(plaintext, StandardCharsets.UTF_8));

        System.out.println("\nPublic directory:");
        server.getPublicDirectory().forEach((id, vector) ->
                System.out.println(id + " -> " + Arrays.toString(vector)));

        System.out.println("\nSecret matrix on server (kept hidden from clients):\n" +
                VectorUtils.formatMatrix(server.getSecretMatrix()));

        System.out.println("\nKPS demo complete!\n");
    }

    private String prompt(String label, String defaultValue) {
        System.out.print(label + " [" + defaultValue + "]: ");
        String input = scanner.nextLine().trim();
        if (input.isEmpty()) {
            return defaultValue;
        }
        return input;
    }

    private double promptDouble(String label, double defaultValue) {
        while (true) {
            String input = prompt(label, Double.toString(defaultValue));
            try {
                return Double.parseDouble(input);
            } catch (NumberFormatException e) {
                System.out.println("Please enter a valid number.");
            }
        }
    }

    private int promptInt(String label, int defaultValue) {
        while (true) {
            String input = prompt(label, Integer.toString(defaultValue));
            try {
                return Integer.parseInt(input);
            } catch (NumberFormatException e) {
                System.out.println("Please enter a whole number.");
            }
        }
    }
}

package com.example.blockchain;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;

/**
 * A simple blockchain client.
 * It can:
 *  1. Generate its own key pair.
 *  2. Create and sign a transaction.
 *  3. Save the transaction to a file.
 *  4. Submit the transaction to a FullNode and get notified when it is confirmed.
 */
public class Client {

    // The name of the user (e.g., "Alice")
    private final String name;

    // Public + private RSA keys for signing transactions
    private final KeyPair keyPair;

    // When a client is created, generate a fresh RSA key pair
    public Client(String name) {
        this.name = name;
        this.keyPair = CryptoUtils.generateKeyPair(); // Create RSA keys
    }

    /**
     * Creates a new signed transaction.
     * Steps:
     *  1. Build an "unsigned" transaction just to get its payload.
     *  2. Sign the payload with the private key.
     *  3. Create a new Transaction that contains the signature.
     */
    public Transaction createSignedTransaction(String recipient, double amount) {

        // Create a temporary transaction with an empty signature
        Transaction unsigned = new Transaction(
                name,               // sender name
                recipient,          // recipient name
                amount,             // amount sent
                keyPair.getPublic(),// sender's public key
                ""                  // no signature yet
        );

        // Create digital signature using the private key
        String signature = CryptoUtils.sign(unsigned.payload(), keyPair.getPrivate());

        // Return the final signed transaction
        return new Transaction(
                name,
                recipient,
                amount,
                keyPair.getPublic(),
                signature            // now includes a valid signature
        );
    }

    /**
     * Writes the transaction into a text file on disk.
     * The file will contain lines like:
     *   sender=Alice
     *   recipient=Bob
     *   amount=12.50
     *   publicKey=...
     *   signature=...
     */
    public Path writeTransactionToFile(Transaction transaction, Path file) {
        try {
            Files.writeString(file, transaction.toFileFormat());
            return file; // return the same path for convenience
        } catch (IOException e) {
            throw new IllegalStateException("Unable to write transaction file", e);
        }
    }

    /**
     * Sends the transaction to a full node.
     * A callback is also registered so the client is notified
     * when the transaction is confirmed inside a mined block.
     */
    public void submitTransaction(FullNode node, Transaction transaction) {

        // Call FullNode.receiveTransaction with a confirmation listener
        node.receiveTransaction(transaction, (tx, block) -> System.out.println(
                name + " learned that tx " + tx.getId() +
                " was confirmed in block #" + block.getIndex() +
                " (hash=" + block.getHash() + ")"
        ));
    }
}
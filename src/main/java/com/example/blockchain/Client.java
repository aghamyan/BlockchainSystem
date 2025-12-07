package com.example.blockchain;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;

/**
 * Lightweight client that creates and signs transactions, writes them to disk, and submits them to a full node.
 */
public class Client {
    private final String name;
    private final KeyPair keyPair;

    public Client(String name) {
        this.name = name;
        this.keyPair = CryptoUtils.generateKeyPair();
    }

    public Transaction createSignedTransaction(String recipient, double amount) {
        Transaction unsigned = new Transaction(name, recipient, amount, keyPair.getPublic(), "");
        String signature = CryptoUtils.sign(unsigned.payload(), keyPair.getPrivate());
        return new Transaction(name, recipient, amount, keyPair.getPublic(), signature);
    }

    public Path writeTransactionToFile(Transaction transaction, Path file) {
        try {
            Files.writeString(file, transaction.toFileFormat());
            return file;
        } catch (IOException e) {
            throw new IllegalStateException("Unable to write transaction file", e);
        }
    }

    public void submitTransaction(FullNode node, Transaction transaction) {
        node.receiveTransaction(transaction, (tx, block) -> System.out.println(
                name + " learned that tx " + tx.getId() + " was confirmed in block #" + block.getIndex() +
                        " (hash=" + block.getHash() + ")"));
    }
}

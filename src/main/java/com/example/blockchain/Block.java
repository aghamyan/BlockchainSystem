package com.example.blockchain;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a single block in the blockchain.
 * 
 * A block contains:
 *   - index         → position in the chain
 *   - previousHash  → hash of the previous block (chain link)
 *   - timestamp     → when block was created
 *   - transactions  → list of transactions stored in this block
 *   - nonce         → number used to find a valid Proof of Work
 *   - hash          → resulting hash after mining
 */
public class Block {

    // Position of the block in the blockchain
    private final int index;

    // Hash of the previous block (chain linking)
    private final String previousHash;

    // When the block was created
    private final long timestamp;

    // List of transactions included in this block
    private final List<Transaction> transactions;

    // Mining fields
    private long nonce;         // Value found during mining that satisfies PoW
    private String hash;        // Final block hash after mining

    /**
     * Constructor that uses the current system time as the timestamp.
     */
    public Block(int index, String previousHash, List<Transaction> transactions) {
        this(index, previousHash, transactions, Instant.now().toEpochMilli());
    }

    /**
     * Constructor used when timestamp needs to be specified manually.
     */
    public Block(int index, String previousHash, List<Transaction> transactions, long timestamp) {
        this.index = index;
        this.previousHash = previousHash;
        this.transactions = new ArrayList<>(transactions); // defensive copy
        this.timestamp = timestamp;
    }

    // -------------------------
    // Getters
    // -------------------------

    public int getIndex() {
        return index;
    }

    public String getPreviousHash() {
        return previousHash;
    }

    public long getTimestamp() {
        return timestamp;
    }

    // Returns a safe copy to prevent external mutation
    public List<Transaction> getTransactions() {
        return new ArrayList<>(transactions);
    }

    public long getNonce() {
        return nonce;
    }

    public String getHash() {
        return hash;
    }

    // -------------------------
    // Setters used during mining
    // -------------------------

    public void setNonce(long nonce) {
        this.nonce = nonce;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    /**
     * Builds the block content string for hashing, using a test nonce.
     *
     * The hash input includes:
     *   - index
     *   - previousHash
     *   - timestamp
     *   - trialNonce (the nonce we are testing)
     *   - all transaction IDs
     *
     * This ensures:
     *   - block cannot be modified without changing its hash
     *   - nonce determines whether the PoW requirement is satisfied
     */
    public String contentWithNonce(long trialNonce) {
        StringBuilder builder = new StringBuilder();

        builder.append(index)
               .append(previousHash)
               .append(timestamp)
               .append(trialNonce);

        // Include all transaction IDs to bind them into the block hash
        for (Transaction tx : transactions) {
            builder.append(tx.getId());
        }

        return builder.toString();
    }
}
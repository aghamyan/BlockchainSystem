package com.example.blockchain;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Basic block that relies on Proof of Work to become valid.
 */
public class Block {
    private final int index;
    private final String previousHash;
    private final long timestamp;
    private final List<Transaction> transactions;
    private long nonce;
    private String hash;

    public Block(int index, String previousHash, List<Transaction> transactions) {
        this(index, previousHash, transactions, Instant.now().toEpochMilli());
    }

    public Block(int index, String previousHash, List<Transaction> transactions, long timestamp) {
        this.index = index;
        this.previousHash = previousHash;
        this.transactions = new ArrayList<>(transactions);
        this.timestamp = timestamp;
    }

    public int getIndex() {
        return index;
    }

    public String getPreviousHash() {
        return previousHash;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public List<Transaction> getTransactions() {
        return new ArrayList<>(transactions);
    }

    public long getNonce() {
        return nonce;
    }

    public String getHash() {
        return hash;
    }

    public void setNonce(long nonce) {
        this.nonce = nonce;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    public String contentWithNonce(long trialNonce) {
        StringBuilder builder = new StringBuilder();
        builder.append(index)
                .append(previousHash)
                .append(timestamp)
                .append(trialNonce);
        for (Transaction tx : transactions) {
            builder.append(tx.getId());
        }
        return builder.toString();
    }
}

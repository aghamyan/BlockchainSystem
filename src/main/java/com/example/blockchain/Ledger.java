package com.example.blockchain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The Ledger represents the blockchain itself.
 * It stores the ordered list of blocks and validates new incoming blocks.
 */
public class Ledger {

    // The list of blocks in the blockchain
    private final List<Block> chain = new ArrayList<>();

    // Mining difficulty (number of leading zeros required in block hash)
    private final int difficulty;

    /**
     * Constructor.
     * Creates the ledger and initializes it with the **genesis block**.
     */
    public Ledger(int difficulty) {
        this.difficulty = difficulty;

        // Create the genesis block (index 0, previous hash "0", empty transactions)
        Block genesis = new Block(0, "0", List.of(), 0L);

        // Even the genesis block is mined using Proof of Work
        ProofOfWorkMiner.mine(genesis, difficulty);

        // Add genesis block to the chain
        chain.add(genesis);
    }

    /**
     * Returns the latest (most recent) block in the blockchain.
     * synchronized → thread-safe access if multiple threads use the node
     */
    public synchronized Block getLatestBlock() {
        return chain.get(chain.size() - 1);
    }

    /**
     * Attempts to add a new block to the chain.
     * It checks:
     *   1. If the block’s previousHash matches the latest block's hash.
     *   2. If the block satisfies the Proof of Work requirement.
     *
     * If both are valid, the block is added; otherwise it is rejected.
     */
    public synchronized boolean addBlock(Block block) {
        Block latest = getLatestBlock();

        // Check 1 — Does block correctly reference previous block?
        boolean hasCorrectPrev = latest.getHash().equals(block.getPreviousHash());

        // Check 2 — Does the block's hash satisfy the PoW difficulty?
        boolean validPow = ProofOfWorkMiner.isValid(block, difficulty);

        // If both conditions are true, accept and append the block
        if (hasCorrectPrev && validPow) {
            chain.add(block);
            return true;
        }

        // Otherwise, reject the block
        return false;
    }

    /**
     * Returns a **read-only copy** of the blockchain.
     * This prevents external code from modifying the real chain.
     */
    public synchronized List<Block> snapshot() {
        return Collections.unmodifiableList(new ArrayList<>(chain));
    }

    /**
     * Returns the mining difficulty.
     */
    public int getDifficulty() {
        return difficulty;
    }
}
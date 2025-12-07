package com.example.blockchain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Maintains the chain of blocks.
 */
public class Ledger {
    private final List<Block> chain = new ArrayList<>();
    private final int difficulty;

    public Ledger(int difficulty) {
        this.difficulty = difficulty;
        Block genesis = new Block(0, "0", List.of(), 0L);
        ProofOfWorkMiner.mine(genesis, difficulty);
        chain.add(genesis);
    }

    public synchronized Block getLatestBlock() {
        return chain.get(chain.size() - 1);
    }

    public synchronized boolean addBlock(Block block) {
        Block latest = getLatestBlock();
        boolean hasCorrectPrev = latest.getHash().equals(block.getPreviousHash());
        boolean validPow = ProofOfWorkMiner.isValid(block, difficulty);
        if (hasCorrectPrev && validPow) {
            chain.add(block);
            return true;
        }
        return false;
    }

    public synchronized List<Block> snapshot() {
        return Collections.unmodifiableList(new ArrayList<>(chain));
    }

    public int getDifficulty() {
        return difficulty;
    }
}

package com.example.blockchain;

/**
 * Simple Proof of Work miner that brute-forces a nonce until the hash has a desired prefix.
 */
public class ProofOfWorkMiner {
    public static void mine(Block block, int difficulty) {
        String targetPrefix = "0".repeat(difficulty);
        long nonce = 0;
        while (true) {
            String content = block.contentWithNonce(nonce);
            String hash = CryptoUtils.sha256(content);
            if (hash.startsWith(targetPrefix)) {
                block.setNonce(nonce);
                block.setHash(hash);
                return;
            }
            nonce++;
        }
    }

    public static boolean isValid(Block block, int difficulty) {
        String targetPrefix = "0".repeat(difficulty);
        String recalculated = CryptoUtils.sha256(block.contentWithNonce(block.getNonce()));
        return recalculated.equals(block.getHash()) && block.getHash() != null && block.getHash().startsWith(targetPrefix);
    }
}

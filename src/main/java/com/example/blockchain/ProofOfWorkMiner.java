package com.example.blockchain;

/**
 * Simple Proof of Work miner that tries different nonce values
 * until it finds a hash that begins with a required number of zeros.
 */
public class ProofOfWorkMiner {

    /**
     * Mines a block using Proof of Work.
     * The goal: find a nonce so that the block's hash starts with `difficulty` number of "0"s.
     */
    public static void mine(Block block, int difficulty) {

        // The required hash prefix. For difficulty = 4, this becomes "0000".
        String targetPrefix = "0".repeat(difficulty);

        long nonce = 0; // Start checking from nonce = 0

        while (true) {

            // Combine block data + nonce into a string
            String content = block.contentWithNonce(nonce);

            // Compute SHA-256 hash of this content
            String hash = CryptoUtils.sha256(content);

            // If the hash begins with "0000" (or however many zeros), PoW is satisfied
            if (hash.startsWith(targetPrefix)) {

                // Save the winning nonce and hash into the block
                block.setNonce(nonce);
                block.setHash(hash);

                // Mining is complete
                return;
            }

            // Otherwise try the next nonce
            nonce++;
        }
    }

    /**
     * Validates that a block still satisfies Proof of Work.
     * Checks:
     *   1. Recomputing the hash using the saved nonce produces the same hash.
     *   2. Hash begins with the required number of zeros.
     */
    public static boolean isValid(Block block, int difficulty) {

        // Expected prefix again (e.g., "0000")
        String targetPrefix = "0".repeat(difficulty);

        // Recalculate the block hash using the saved nonce
        String recalculated = CryptoUtils.sha256(block.contentWithNonce(block.getNonce()));

        // Block is valid only if:
        //   - recalculated hash matches block.getHash()
        //   - block hash is not null
        //   - block hash starts with correct number of zeros
        return recalculated.equals(block.getHash())
                && block.getHash() != null
                && block.getHash().startsWith(targetPrefix);
    }
}
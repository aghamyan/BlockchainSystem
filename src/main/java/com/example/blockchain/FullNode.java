package com.example.blockchain;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A full node maintains the ledger, mines Proof of Work blocks,
 * receives/broadcasts transactions, and notifies clients when
 * their transactions are confirmed.
 */
public class FullNode {

    // Unique name/ID of this node (e.g., "full-node-A")
    private final String nodeId;

    // The blockchain ledger (genesis block is created inside Ledger)
    private final Ledger ledger;

    // List of all unconfirmed transactions waiting to be mined
    private final List<Transaction> mempool = new ArrayList<>();

    // Connected peers (other full nodes in the network)
    private final Set<FullNode> peers = new HashSet<>();

    // Stores confirmation listeners for each transaction ID
    // Example: listeners.get(txId) → list of callbacks
    private final Map<String, List<ConfirmationListener>> listeners = new HashMap<>();

    // Keeps track of transaction IDs this node already processed
    // Prevents duplicate work and infinite loops
    private final Set<String> seenTransactions = new HashSet<>();

    // Keeps track of block hashes this node already processed
    private final Set<String> seenBlocks = new HashSet<>();


    /**
     * Create a new full node.
     * It initializes a Ledger (which creates a genesis block)
     * and marks the genesis block as "seen".
     */
    public FullNode(String nodeId, int difficulty) {
        this.nodeId = nodeId;
        this.ledger = new Ledger(difficulty); // Creates genesis block
        seenBlocks.add(ledger.getLatestBlock().getHash());
    }

    public String getNodeId() {
        return nodeId;
    }

    public Ledger getLedger() {
        return ledger;
    }


    /**
     * Connects this node with another peer.
     * The connection is automatically bidirectional.
     */
    public void connectPeer(FullNode peer) {
        if (peer == this) { // avoid connecting to itself
            return;
        }
        peers.add(peer);
        peer.peers.add(this); // make connection both ways
    }


    /**
     * Public method used by clients to send a transaction.
     * It delegates processing to handleTransaction().
     */
    public void receiveTransaction(Transaction transaction, ConfirmationListener listener) {
        handleTransaction(transaction, listener, false);
    }


    /**
     * Internal method that:
     *  - Validates signatures
     *  - Prevents duplicates
     *  - Stores transaction in the mempool
     *  - Saves confirmation listeners
     *  - Broadcasts the transaction to other nodes (if needed)
     */
    private void handleTransaction(Transaction transaction,
                                   ConfirmationListener listener,
                                   boolean fromPeer) {

        // Check digital signature validity (sender actually signed it)
        if (!transaction.isSignatureValid()) {
            System.out.println(nodeId + " rejected invalid transaction from " + transaction.getSender());
            return;
        }

        // If we have already processed this transaction, ignore it
        if (!seenTransactions.add(transaction.getId())) {
            return; // Already seen
        }

        // Store in mempool (waiting to be mined)
        mempool.add(transaction);

        // If client provided a listener, save it so we can notify them later
        if (listener != null) {
            listeners
                .computeIfAbsent(transaction.getId(), k -> new ArrayList<>())
                .add(listener);
        }

        // If transaction did NOT come from another peer, broadcast it
        if (!fromPeer) {
            broadcastTransaction(transaction);
        }
    }


    /**
     * Send the transaction to all neighboring nodes.
     */
    private void broadcastTransaction(Transaction transaction) {
        for (FullNode peer : peers) {
            // fromPeer = true so they don't re-broadcast infinitely
            peer.handleTransaction(transaction, null, true);
        }
    }


    /**
     * Mines all pending transactions in the mempool into a new block.
     * Uses Proof of Work to find a valid hash.
     */
    public void minePendingTransactions() {

        // If nothing to mine, stop here
        if (mempool.isEmpty()) {
            System.out.println(nodeId + " nothing to mine");
            return;
        }

        // Get last block
        Block latest = ledger.getLatestBlock();

        // Create a new block candidate with everything in mempool
        Block candidate = new Block(
                latest.getIndex() + 1,
                latest.getHash(),
                new ArrayList<>(mempool)
        );

        System.out.println(nodeId + " mining block with " + mempool.size() + " tx(s)...");

        // Try nonces until hash meets difficulty (e.g. "0000...")
        ProofOfWorkMiner.mine(candidate, ledger.getDifficulty());

        // If the block is valid and fits the chain, add it
        if (ledger.addBlock(candidate)) {

            // Mark block as seen
            seenBlocks.add(candidate.getHash());

            // Notify clients that their transactions were confirmed
            notifyConfirmations(candidate);

            // Clear mempool now that transactions are confirmed
            mempool.clear();

            // Broadcast the block to other nodes
            broadcastBlock(candidate);

            System.out.println(nodeId + " mined block #" + candidate.getIndex() +
                    " (hash=" + candidate.getHash() + ")");
        }
    }


    /**
     * After mining, notify all clients that their transaction is confirmed.
     */
    private void notifyConfirmations(Block block) {
        for (Transaction tx : block.getTransactions()) {
            List<ConfirmationListener> registered = listeners.get(tx.getId());
            if (registered != null) {
                for (ConfirmationListener listener : registered) {
                    listener.onTransactionConfirmed(tx, block);
                }
            }
        }
    }


    /**
     * Broadcast a newly mined block to all peers.
     */
    private void broadcastBlock(Block block) {
        for (FullNode peer : peers) {
            peer.receiveBlock(block, true);
        }
    }


    /**
     * Called when a peer sends this node a block.
     * Validates the block and, if correct, adds it to the chain.
     */
    public void receiveBlock(Block block, boolean fromPeer) {

        // If we already processed this block, ignore it
        if (!seenBlocks.add(block.getHash())) {
            return;
        }

        // Try to add the block to the ledger
        boolean accepted = ledger.addBlock(block);

        if (accepted) {
            // Remove included transactions from mempool
            mempool.removeAll(block.getTransactions());

            // Notify clients
            notifyConfirmations(block);

            // Broadcast further if necessary
            if (!fromPeer) {
                broadcastBlock(block);
            }
        } else {
            // Block rejected → remove from seenBlocks
            seenBlocks.remove(block.getHash());
        }
    }


    /**
     * Return a read-only snapshot of the blockchain.
     */
    public List<Block> getChainSnapshot() {
        return ledger.snapshot();
    }
}
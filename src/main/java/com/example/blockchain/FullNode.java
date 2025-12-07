package com.example.blockchain;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A full node maintains the ledger, mines Proof of Work blocks, and relays transactions and confirmations.
 */
public class FullNode {
    private final String nodeId;
    private final Ledger ledger;
    private final List<Transaction> mempool = new ArrayList<>();
    private final Set<FullNode> peers = new HashSet<>();
    private final Map<String, List<ConfirmationListener>> listeners = new HashMap<>();
    private final Set<String> seenTransactions = new HashSet<>();
    private final Set<String> seenBlocks = new HashSet<>();

    public FullNode(String nodeId, int difficulty) {
        this.nodeId = nodeId;
        this.ledger = new Ledger(difficulty);
        seenBlocks.add(ledger.getLatestBlock().getHash());
    }

    public String getNodeId() {
        return nodeId;
    }

    public Ledger getLedger() {
        return ledger;
    }

    public void connectPeer(FullNode peer) {
        if (peer == this) {
            return;
        }
        peers.add(peer);
        peer.peers.add(this);
    }

    public void receiveTransaction(Transaction transaction, ConfirmationListener listener) {
        handleTransaction(transaction, listener, false);
    }

    private void handleTransaction(Transaction transaction, ConfirmationListener listener, boolean fromPeer) {
        if (!transaction.isSignatureValid()) {
            System.out.println(nodeId + " rejected invalid transaction from " + transaction.getSender());
            return;
        }

        if (!seenTransactions.add(transaction.getId())) {
            return; // Already processed
        }

        mempool.add(transaction);
        if (listener != null) {
            listeners.computeIfAbsent(transaction.getId(), k -> new ArrayList<>()).add(listener);
        }

        if (!fromPeer) {
            broadcastTransaction(transaction);
        }
    }

    private void broadcastTransaction(Transaction transaction) {
        for (FullNode peer : peers) {
            peer.handleTransaction(transaction, null, true);
        }
    }

    public void minePendingTransactions() {
        if (mempool.isEmpty()) {
            System.out.println(nodeId + " nothing to mine");
            return;
        }

        Block latest = ledger.getLatestBlock();
        Block candidate = new Block(latest.getIndex() + 1, latest.getHash(), new ArrayList<>(mempool));
        System.out.println(nodeId + " mining block with " + mempool.size() + " tx(s)...");
        ProofOfWorkMiner.mine(candidate, ledger.getDifficulty());

        if (ledger.addBlock(candidate)) {
            seenBlocks.add(candidate.getHash());
            notifyConfirmations(candidate);
            mempool.clear();
            broadcastBlock(candidate);
            System.out.println(nodeId + " mined block #" + candidate.getIndex() + " (hash=" + candidate.getHash() + ")");
        }
    }

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

    private void broadcastBlock(Block block) {
        for (FullNode peer : peers) {
            peer.receiveBlock(block, true);
        }
    }

    public void receiveBlock(Block block, boolean fromPeer) {
        if (!seenBlocks.add(block.getHash())) {
            return;
        }

        boolean accepted = ledger.addBlock(block);
        if (accepted) {
            mempool.removeAll(block.getTransactions());
            notifyConfirmations(block);
            if (!fromPeer) {
                broadcastBlock(block);
            }
        } else {
            seenBlocks.remove(block.getHash());
        }
    }

    public List<Block> getChainSnapshot() {
        return ledger.snapshot();
    }
}

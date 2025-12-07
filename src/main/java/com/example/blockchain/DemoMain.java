package com.example.blockchain;

import java.nio.file.Path;

/**
 * Demonstrates a tiny blockchain network with multiple full nodes and a client.
 */
public class DemoMain {
    public static void main(String[] args) {
        // Build a network of interconnected full nodes
        FullNode nodeA = new FullNode("full-node-A", 4);
        FullNode nodeB = new FullNode("full-node-B", 4);
        FullNode nodeC = new FullNode("full-node-C", 4);
        nodeA.connectPeer(nodeB);
        nodeB.connectPeer(nodeC);

        // Client prepares a signed transaction and writes it to disk
        Client alice = new Client("Alice");
        Transaction tx = alice.createSignedTransaction("Bob", 12.50);
        Path txFile = Path.of("alice_to_bob.txn");
        alice.writeTransactionToFile(tx, txFile);
        System.out.println("Transaction file written to " + txFile.toAbsolutePath());

        // The signed transaction is submitted to a full node
        alice.submitTransaction(nodeA, tx);

        // Any node can mine; pick node B to confirm the transaction
        nodeB.minePendingTransactions();

        // Demonstrate that all nodes share the same ledger height
        System.out.println("\nLedger heights after mining:");
        System.out.println(nodeA.getNodeId() + " blocks: " + nodeA.getChainSnapshot().size());
        System.out.println(nodeB.getNodeId() + " blocks: " + nodeB.getChainSnapshot().size());
        System.out.println(nodeC.getNodeId() + " blocks: " + nodeC.getChainSnapshot().size());
    }
}

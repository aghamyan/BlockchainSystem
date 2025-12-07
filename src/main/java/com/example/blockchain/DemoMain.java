package com.example.blockchain;

import java.nio.file.Path;

/**
 * Demonstrates a tiny blockchain network with multiple full nodes and a client.
 * This is the "demo" program that shows how all other classes work together.
 */
public class DemoMain {
    public static void main(String[] args) {

        // ------------------------------
        // 1. Create three full nodes
        // ------------------------------
        // Each node has:
        //   - an ID
        //   - a difficulty for mining (4 leading zeroes)
        FullNode nodeA = new FullNode("full-node-A", 4);
        FullNode nodeB = new FullNode("full-node-B", 4);
        FullNode nodeC = new FullNode("full-node-C", 4);

        // Connect nodes like a small peer-to-peer network:
        // A <-> B <-> C
        nodeA.connectPeer(nodeB);
        nodeB.connectPeer(nodeC);

        // ------------------------------
        // 2. Create a client ("Alice")
        // ------------------------------
        Client alice = new Client("Alice");

        // Alice creates and signs a transaction sending 12.50 to Bob
        Transaction tx = alice.createSignedTransaction("Bob", 12.50);

        // Write transaction text into a .txn file
        Path txFile = Path.of("alice_to_bob.txn");
        alice.writeTransactionToFile(tx, txFile);

        System.out.println("Transaction file written to " + txFile.toAbsolutePath());

        // ------------------------------
        // 3. Alice submits her transaction to node A
        // ------------------------------
        // Node A will validate it and broadcast it to B and C.
        // Alice also registers a callback so she knows when it is confirmed.
        alice.submitTransaction(nodeA, tx);

        // ------------------------------
        // 4. Mine the pending transaction
        // ------------------------------
        // Any node can mine. Here we choose node B to mine a new block.
        nodeB.minePendingTransactions();

        // ------------------------------
        // 5. Show that all nodes now share the same ledger state
        // ------------------------------
        // Each node should have:
        //   - Genesis block
        //   - Newly mined block (with Alice's transaction)
        System.out.println("\nLedger heights after mining:");
        System.out.println(nodeA.getNodeId() + " blocks: " + nodeA.getChainSnapshot().size());
        System.out.println(nodeB.getNodeId() + " blocks: " + nodeB.getChainSnapshot().size());
        System.out.println(nodeC.getNodeId() + " blocks: " + nodeC.getChainSnapshot().size());
    }
}
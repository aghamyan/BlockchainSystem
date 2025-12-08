# BlockchainSystem

This project demonstrates a lightweight blockchain network in Java. It models a set of interconnected full nodes that maintain a shared ledger with Proof of Work, relay transactions, and notify clients when their transfers are confirmed. A simple client generates signed transaction files and submits them to a chosen node for inclusion in the ledger.

## Components
- **Client** (`com.example.blockchain.Client`) generates an RSA key pair, signs transaction payloads, writes them to disk, and submits them to a full node.
- **Transaction** (`com.example.blockchain.Transaction`) stores sender/recipient/amount information alongside the sender's public key and signature. Transactions can be serialized to or reconstructed from a text file.
- **Full Node** (`com.example.blockchain.FullNode`) keeps a ledger, exchanges transactions and blocks with peer nodes, mines blocks with Proof of Work, and calls back to clients when their transactions are confirmed.
- **Ledger** (`com.example.blockchain.Ledger`) is the canonical chain of blocks each protected by Proof of Work.
- **Proof of Work** (`com.example.blockchain.ProofOfWorkMiner`) brute-forces nonces until the block hash starts with the required number of leading zeroes.

## Demos
Two runnable demos are included:

- **Blockchain** (`com.example.blockchain.DemoMain`) builds a three-node network, has a client create and sign a transaction file, submits it to a node, mines a block, and shows that all full nodes converge on the same chain height.
- **Key Predistribution System (KPS)** (`com.example.kps.demo.DemoMain`) provisions users with key material so they can derive matching shared keys without talking to the server again.

## Build and run
```
# Compile all sources
devon@machine:$ javac -d out $(find src/main/java -name "*.java")

# Run the interactive console UI (offers both demos)
devon@machine:$ java -cp out com.example.AppUI

# Launch the graphical UI with tabs for each demo
devon@machine:$ java -cp out com.example.AppSwingUI

# Run individual demos directly
devon@machine:$ java -cp out com.example.blockchain.DemoMain
devon@machine:$ java -cp out com.example.kps.demo.DemoMain
```

The interactive UI prompts for sender/recipient names, amounts, and difficulty for the blockchain demo, and for user IDs and a message for the KPS demo. Outputs include the path to the generated transaction file, Proof of Work mining progress, key material summaries, and confirmation notifications.

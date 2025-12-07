# BlockchainSystem

This demo implements a Blom-like Key Predistribution System (KPS) in Java.

## How it works
- **Server** (`KeyServer`) generates a symmetric master matrix modulo a large prime and derives each user's public vector and secret share (`KeyMaterial`).
- **Provisioning** (`ProvisioningService`) registers users and serializes their predistributed material.
- **Clients** (`UserClient`) compute the shared scalar `A_i * S * A_j^T mod p` using their secret share and the other party's public vector, then derive an AES key via SHA-256 (`KeyDerivation`).
- **Demo** (`DemoMain`) shows Alice and Bob deriving the same key and using it to encrypt/decrypt a message.

The scheme is symmetric: without the predistributed share from the server, an adversary cannot reconstruct the master matrix or compute the shared key.

## Build and run

```
# Compile
javac -d out $(find src/main/java -name "*.java")

# Run demo
java -cp out com.example.kps.demo.DemoMain
```

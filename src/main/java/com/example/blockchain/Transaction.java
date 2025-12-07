package com.example.blockchain;

import java.security.PublicKey;
import java.text.DecimalFormat;
import java.util.Objects;

/**
 * Represents a signed transfer between two parties.
 * A transaction contains:
 *   - sender name
 *   - recipient name
 *   - amount of money
 *   - sender’s public key (to verify signature)
 *   - cryptographic signature
 */
public class Transaction {

    // Ensures amounts always look like "12.50"
    private static final DecimalFormat AMOUNT_FORMAT = new DecimalFormat("0.00");

    // Basic transaction fields
    private final String sender;
    private final String recipient;
    private final double amount;
    private final PublicKey senderKey;  // used to verify the signature
    private final String signature;     // Base64-encoded digital signature

    /**
     * Creates a transaction.
     * This class does NOT sign the transaction — the Client signs it.
     */
    public Transaction(String sender, String recipient, double amount,
                       PublicKey senderKey, String signature) {
        this.sender = sender;
        this.recipient = recipient;
        this.amount = amount;
        this.senderKey = senderKey;
        this.signature = signature;
    }

    // Getters
    public String getSender() {
        return sender;
    }

    public String getRecipient() {
        return recipient;
    }

    public double getAmount() {
        return amount;
    }

    public String getSignature() {
        return signature;
    }

    public PublicKey getSenderKey() {
        return senderKey;
    }

    /**
     * Returns the message that is signed.
     * The signature covers ONLY this payload.
     *
     * Example output:
     *    "Alice:Bob:12.50"
     */
    public String payload() {
        return sender + ":" + recipient + ":" + AMOUNT_FORMAT.format(amount);
    }

    /**
     * Validates the signature using the sender’s stored public key.
     * Returns true only if the signature matches the payload exactly.
     */
    public boolean isSignatureValid() {
        return CryptoUtils.verify(payload(), signature, senderKey);
    }

    /**
     * Transaction ID = hash(payload + signature)
     * This ensures every signed transaction produces a unique identifier.
     */
    public String getId() {
        return CryptoUtils.sha256(payload() + signature);
    }

    /**
     * Converts the transaction into a readable text format.
     * Used when writing the transaction into a .txn file.
     */
    public String toFileFormat() {
        return "sender=" + sender + "\n" +
                "recipient=" + recipient + "\n" +
                "amount=" + AMOUNT_FORMAT.format(amount) + "\n" +
                "publicKey=" + CryptoUtils.encodePublicKey(senderKey) + "\n" +
                "signature=" + signature + "\n";
    }

    /**
     * Reconstructs a Transaction object from a saved text file.
     * Reads each line, extracts fields, and rebuilds the transaction.
     */
    public static Transaction fromFileContent(String content) {

        String senderValue = null;
        String recipientValue = null;
        Double amountValue = null;
        String publicKeyValue = null;
        String signatureValue = null;

        // Split into lines and parse each line
        String[] lines = content.split("\n");
        for (String line : lines) {

            if (line.startsWith("sender=")) {
                senderValue = line.substring("sender=".length());

            } else if (line.startsWith("recipient=")) {
                recipientValue = line.substring("recipient=".length());

            } else if (line.startsWith("amount=")) {
                amountValue = Double.parseDouble(line.substring("amount=".length()));

            } else if (line.startsWith("publicKey=")) {
                publicKeyValue = line.substring("publicKey=".length());

            } else if (line.startsWith("signature=")) {
                signatureValue = line.substring("signature=".length());
            }
        }

        // If any field is missing → file is invalid
        if (senderValue == null || recipientValue == null || amountValue == null ||
                publicKeyValue == null || signatureValue == null) {
            throw new IllegalArgumentException("Missing transaction fields in file");
        }

        // Rebuild transaction using the decoded public key
        return new Transaction(
                senderValue,
                recipientValue,
                amountValue,
                CryptoUtils.decodePublicKey(publicKeyValue),
                signatureValue
        );
    }

    /**
     * Two transactions are considered equal if their IDs are equal.
     * This prevents duplicates in mempool and ledger.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Transaction)) return false;
        Transaction that = (Transaction) o;
        return Objects.equals(getId(), that.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getId());
    }
}
package com.example.blockchain;

import java.security.PublicKey;
import java.text.DecimalFormat;
import java.util.Objects;

/**
 * Represents a signed transfer between two parties.
 */
public class Transaction {
    private static final DecimalFormat AMOUNT_FORMAT = new DecimalFormat("0.00");

    private final String sender;
    private final String recipient;
    private final double amount;
    private final PublicKey senderKey;
    private final String signature;

    public Transaction(String sender, String recipient, double amount, PublicKey senderKey, String signature) {
        this.sender = sender;
        this.recipient = recipient;
        this.amount = amount;
        this.senderKey = senderKey;
        this.signature = signature;
    }

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

    public String payload() {
        return sender + ":" + recipient + ":" + AMOUNT_FORMAT.format(amount);
    }

    public boolean isSignatureValid() {
        return CryptoUtils.verify(payload(), signature, senderKey);
    }

    public String getId() {
        return CryptoUtils.sha256(payload() + signature);
    }

    public String toFileFormat() {
        return "sender=" + sender + "\n" +
                "recipient=" + recipient + "\n" +
                "amount=" + AMOUNT_FORMAT.format(amount) + "\n" +
                "publicKey=" + CryptoUtils.encodePublicKey(senderKey) + "\n" +
                "signature=" + signature + "\n";
    }

    public static Transaction fromFileContent(String content) {
        String senderValue = null;
        String recipientValue = null;
        Double amountValue = null;
        String publicKeyValue = null;
        String signatureValue = null;

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

        if (senderValue == null || recipientValue == null || amountValue == null || publicKeyValue == null || signatureValue == null) {
            throw new IllegalArgumentException("Missing transaction fields in file");
        }

        return new Transaction(senderValue, recipientValue, amountValue,
                CryptoUtils.decodePublicKey(publicKeyValue), signatureValue);
    }

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

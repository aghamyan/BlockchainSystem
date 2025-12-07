package com.example.blockchain;

/**
 * Callback that allows full nodes to inform clients when a transaction is confirmed.
 */
public interface ConfirmationListener {
    void onTransactionConfirmed(Transaction transaction, Block block);
}

package com.example;

import com.example.blockchain.Client;
import com.example.blockchain.FullNode;
import com.example.blockchain.Transaction;
import com.example.kps.client.UserClient;
import com.example.kps.crypto.KeyDerivation;
import com.example.kps.server.KeyMaterial;
import com.example.kps.server.KeyServer;
import com.example.kps.server.ProvisioningService;
import com.example.kps.server.crypto.VectorUtils;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Map;

/**
 * Swing-based UI that exposes interactive controls for both the blockchain and
 * Key Predistribution System demos. This windowed interface mirrors the console
 * flow from {@link AppUI} but renders the results in a scrollable text area so
 * the full flow can be demonstrated without relying on stdin/stdout.
 */
public class AppSwingUI extends JFrame {

    private final JTextArea logArea = new JTextArea(20, 70);

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new AppSwingUI().setVisible(true));
    }

    public AppSwingUI() {
        super("Cryptography Playground (GUI)");
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        setLocationByPlatform(true);

        logArea.setEditable(false);
        logArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        JScrollPane scrollPane = new JScrollPane(logArea);

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Blockchain", createBlockchainPanel());
        tabs.addTab("KPS", createKpsPanel());

        add(tabs, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);

        pack();
    }

    private JPanel createBlockchainPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.anchor = GridBagConstraints.WEST;

        JTextField senderField = new JTextField("Alice", 10);
        JTextField recipientField = new JTextField("Bob", 10);
        JTextField amountField = new JTextField("10.50", 10);
        JSpinner difficultySpinner = new JSpinner(new SpinnerNumberModel(4, 1, 7, 1));

        JButton runButton = new JButton("Run blockchain demo");

        gbc.gridx = 0; gbc.gridy = 0; panel.add(new JLabel("Sender"), gbc);
        gbc.gridx = 1; panel.add(senderField, gbc);

        gbc.gridx = 0; gbc.gridy = 1; panel.add(new JLabel("Recipient"), gbc);
        gbc.gridx = 1; panel.add(recipientField, gbc);

        gbc.gridx = 0; gbc.gridy = 2; panel.add(new JLabel("Amount"), gbc);
        gbc.gridx = 1; panel.add(amountField, gbc);

        gbc.gridx = 0; gbc.gridy = 3; panel.add(new JLabel("Difficulty"), gbc);
        gbc.gridx = 1; panel.add(difficultySpinner, gbc);

        gbc.gridx = 0; gbc.gridy = 4; gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel.add(runButton, gbc);

        runButton.addActionListener(e -> runBlockchainDemo(senderField.getText().trim(),
                recipientField.getText().trim(),
                amountField.getText().trim(),
                (Integer) difficultySpinner.getValue(),
                runButton));

        return panel;
    }

    private JPanel createKpsPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.anchor = GridBagConstraints.WEST;

        JTextField userAField = new JTextField("userAlice", 10);
        JTextField userBField = new JTextField("userBob", 10);
        JTextField messageField = new JTextField("Hello from userAlice", 20);

        JButton runButton = new JButton("Run KPS demo");

        gbc.gridx = 0; gbc.gridy = 0; panel.add(new JLabel("User A"), gbc);
        gbc.gridx = 1; panel.add(userAField, gbc);

        gbc.gridx = 0; gbc.gridy = 1; panel.add(new JLabel("User B"), gbc);
        gbc.gridx = 1; panel.add(userBField, gbc);

        gbc.gridx = 0; gbc.gridy = 2; panel.add(new JLabel("Message"), gbc);
        gbc.gridx = 1; panel.add(messageField, gbc);

        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel.add(runButton, gbc);

        runButton.addActionListener(e -> runKpsDemo(userAField.getText().trim(),
                userBField.getText().trim(),
                messageField.getText().trim(),
                runButton));

        return panel;
    }

    private void runBlockchainDemo(String sender, String recipient, String amountText, int difficulty, JButton sourceButton) {
        double amount;
        try {
            amount = Double.parseDouble(amountText);
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Amount must be a number.", "Invalid input", JOptionPane.ERROR_MESSAGE);
            return;
        }

        sourceButton.setEnabled(false);
        appendLog("\n--- Running blockchain demo ---\n");

        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() {
                try {
                    FullNode nodeA = new FullNode("full-node-A", difficulty);
                    FullNode nodeB = new FullNode("full-node-B", difficulty);
                    FullNode nodeC = new FullNode("full-node-C", difficulty);

                    nodeA.connectPeer(nodeB);
                    nodeB.connectPeer(nodeC);

                    Client client = new Client(sender);
                    Transaction tx = client.createSignedTransaction(recipient, amount);

                    Path txFile = Path.of(sender.toLowerCase() + "_to_" + recipient.toLowerCase() + ".txn");
                    client.writeTransactionToFile(tx, txFile);
                    appendLog("Transaction file written to " + txFile.toAbsolutePath());

                    client.submitTransaction(nodeA, tx);
                    appendLog("Submitted transaction to nodeA. Mining pending transactions...\n");

                    nodeB.minePendingTransactions();

                    appendLog("Ledger heights after mining:");
                    appendLog(nodeA.getNodeId() + " blocks: " + nodeA.getChainSnapshot().size());
                    appendLog(nodeB.getNodeId() + " blocks: " + nodeB.getChainSnapshot().size());
                    appendLog(nodeC.getNodeId() + " blocks: " + nodeC.getChainSnapshot().size());

                    appendLog("\nBlock details (from nodeA):");
                    nodeA.getChainSnapshot().forEach(block -> {
                        appendLog("------------------------------");
                        appendLog("Index: " + block.getIndex());
                        appendLog("Hash:  " + block.getHash());
                        appendLog("Prev:  " + block.getPreviousHash());
                        appendLog("Tx count: " + block.getTransactions().size());
                        block.getTransactions().forEach(t -> appendLog("  - " + t.getSender() + " -> " + t.getRecipient() +
                                " : " + t.getAmount() + " (id " + t.getId() + ")"));
                    });

                    appendLog("\nBlockchain demo complete!\n");
                } catch (Exception ex) {
                    appendLog("Error during blockchain demo: " + ex.getMessage());
                }
                return null;
            }

            @Override
            protected void done() {
                sourceButton.setEnabled(true);
            }
        };

        worker.execute();
    }

    private void runKpsDemo(String userA, String userB, String message, JButton sourceButton) {
        if (userA.isEmpty() || userB.isEmpty()) {
            JOptionPane.showMessageDialog(this, "User IDs cannot be empty.", "Invalid input", JOptionPane.ERROR_MESSAGE);
            return;
        }

        sourceButton.setEnabled(false);
        appendLog("\n--- Running KPS demo ---\n");

        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() {
                try {
                    int prime = 2147483647;
                    int dimension = 4;
                    KeyServer server = new KeyServer(prime, dimension);
                    ProvisioningService provisioning = new ProvisioningService(server);

                    Map<String, String> packets = provisioning.provisionUsers(userA, userB);
                    KeyMaterial materialA = KeyMaterial.deserialize(packets.get(userA));
                    KeyMaterial materialB = KeyMaterial.deserialize(packets.get(userB));

                    UserClient clientA = new UserClient(materialA, server.getPublicDirectory());
                    UserClient clientB = new UserClient(materialB, server.getPublicDirectory());

                    byte[] keyA = clientA.computeSharedKey(userB);
                    byte[] keyB = clientB.computeSharedKey(userA);

                    appendLog("Derived keys (should match):");
                    appendLog(userA + " key: " + KeyDerivation.toHex(keyA));
                    appendLog(userB + " key: " + KeyDerivation.toHex(keyB));
                    appendLog("Keys equal? " + Arrays.equals(keyA, keyB));

                    byte[] ciphertext = KeyDerivation.encryptAes(keyA, message.getBytes(StandardCharsets.UTF_8));
                    byte[] plaintext = KeyDerivation.decryptAes(keyB, ciphertext);

                    appendLog("Ciphertext (hex): " + KeyDerivation.toHex(ciphertext));
                    appendLog("Decrypted text  : " + new String(plaintext, StandardCharsets.UTF_8));

                    appendLog("\nPublic directory:");
                    server.getPublicDirectory().forEach((id, vector) ->
                            appendLog(id + " -> " + Arrays.toString(vector)));

                    appendLog("\nSecret matrix on server (kept hidden from clients):\n" +
                            VectorUtils.formatMatrix(server.getSecretMatrix()));

                    appendLog("\nKPS demo complete!\n");
                } catch (Exception ex) {
                    appendLog("Error during KPS demo: " + ex.getMessage());
                }
                return null;
            }

            @Override
            protected void done() {
                sourceButton.setEnabled(true);
            }
        };

        worker.execute();
    }

    private void appendLog(String text) {
        SwingUtilities.invokeLater(() -> {
            logArea.append(text + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }
}

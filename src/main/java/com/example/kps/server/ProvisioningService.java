package com.example.kps.server;

import java.util.HashMap;
import java.util.Map;

/**
 * Simulates secure provisioning of predistributed key material to clients.
 *
 * In a real-world system, this would be where the server securely
 * delivers each user's private key material over a protected channel.
 *
 * Here, provisioning simply:
 *   - registers the user on the KeyServer
 *   - serializes their KeyMaterial into a string
 *   - returns these strings so clients can deserialize them independently
 */
public class ProvisioningService {

    // The central KeyServer that generates user key material
    private final KeyServer keyServer;

    /**
     * Creates a new provisioning service that uses the given KeyServer.
     */
    public ProvisioningService(KeyServer keyServer) {
        this.keyServer = keyServer;
    }

    /**
     * Provisions multiple users at once.
     *
     * Steps for each userId:
     *   1. Register the user on KeyServer (creates public vector + secret share)
     *   2. Serialize KeyMaterial into a string
     *   3. Return all serialized packets in a map { userId → serializedData }
     *
     * @param userIds An array of user identifiers to provision.
     * @return A map containing serialized KeyMaterial for each user.
     */
    public Map<String, String> provisionUsers(String... userIds) {

        Map<String, String> deliveries = new HashMap<>();

        for (String userId : userIds) {

            // Ask KeyServer to create KeyMaterial for this user
            KeyMaterial material = keyServer.registerUser(userId);

            // Store serialized form (ready to send to client)
            deliveries.put(userId, material.serialize());
        }

        return deliveries;
    }
}
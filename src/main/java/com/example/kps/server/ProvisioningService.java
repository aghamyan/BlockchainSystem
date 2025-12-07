package com.example.kps.server;

import java.util.HashMap;
import java.util.Map;

/**
 * Simulates secure provisioning of predistributed key material to clients.
 */
public class ProvisioningService {
    private final KeyServer keyServer;

    public ProvisioningService(KeyServer keyServer) {
        this.keyServer = keyServer;
    }

    public Map<String, String> provisionUsers(String... userIds) {
        Map<String, String> deliveries = new HashMap<>();
        for (String userId : userIds) {
            KeyMaterial material = keyServer.registerUser(userId);
            deliveries.put(userId, material.serialize());
        }
        return deliveries;
    }
}

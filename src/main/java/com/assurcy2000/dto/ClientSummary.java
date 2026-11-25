package com.assurcy2000.dto;

import com.assurcy2000.model.User;

import java.time.LocalDateTime;

/**
 * Une ligne de la liste des clients (agent/clients.jsp) : le client et son nombre de contrats,
 * qui n'est pas un champ de User. Classe à getters (et pas un record) pour être lue par l'EL des JSP.
 */
public class ClientSummary {

    private final User client;
    private final long contractCount;

    public ClientSummary(User client, long contractCount) {
        this.client = client;
        this.contractCount = contractCount;
    }

    public Long getId() { return client.getId(); }
    public String getFirstName() { return client.getFirstName(); }
    public String getLastName() { return client.getLastName(); }
    public String getEmail() { return client.getEmail(); }
    public String getPhone() { return client.getPhone(); }
    public LocalDateTime getCreatedAt() { return client.getCreatedAt(); }
    public long getContractCount() { return contractCount; }
}

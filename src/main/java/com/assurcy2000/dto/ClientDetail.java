package com.assurcy2000.dto;

import com.assurcy2000.model.Claim;
import com.assurcy2000.model.Contract;
import com.assurcy2000.model.User;

import java.util.List;

/**
 * Fiche d'un client affichée à droite de la liste (attribut "selection" de agent/clients.jsp) :
 * ses coordonnées, ses contrats et ses sinistres, chargés dans la même transaction.
 */
public class ClientDetail {

    private final User client;
    private final List<Contract> contracts;
    private final List<Claim> claims;

    public ClientDetail(User client, List<Contract> contracts, List<Claim> claims) {
        this.client = client;
        this.contracts = contracts;
        this.claims = claims;
    }

    public Long getId() { return client.getId(); }
    public String getFirstName() { return client.getFirstName(); }
    public String getLastName() { return client.getLastName(); }
    public String getEmail() { return client.getEmail(); }
    public String getPhone() { return client.getPhone(); }
    public String getAddress() { return client.getAddress(); }
    public List<Contract> getContracts() { return contracts; }
    public List<Claim> getClaims() { return claims; }
}

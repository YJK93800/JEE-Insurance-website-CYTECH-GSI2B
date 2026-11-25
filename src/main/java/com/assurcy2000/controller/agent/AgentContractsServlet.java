package com.assurcy2000.controller.agent;

import com.assurcy2000.controller.BaseServlet;
import com.assurcy2000.dto.SessionUser;
import com.assurcy2000.dto.request.ContractSearchRequest;
import com.assurcy2000.model.enums.ContractStatus;
import com.assurcy2000.service.ContractService;
import com.assurcy2000.service.InsuranceTypeService;
import com.assurcy2000.util.RequestParams;
import com.assurcy2000.util.Views;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * Liste, recherche et détail des contrats.
 *   GET /agent/contracts?q=…&status=ACTIF&type=3   liste (agent : avec les actions)
 *   GET /agent/contracts?id=12                     détail d'un contrat
 *   GET /admin/contracts[?id=12]                   admin : mêmes pages, consultation seule
 * Créer / modifier : ContractFormServlet ; activer, suspendre, résilier, renouveler : ContractActionServlet.
 */
@WebServlet({"/agent/contracts", "/admin/contracts"})
public class AgentContractsServlet extends BaseServlet {

    private final ContractService contractService = new ContractService();
    private final InsuranceTypeService insuranceTypeService = new InsuranceTypeService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        SessionUser user = user(req);

        // ?id=12 : détail d'un contrat (404 s'il n'existe pas)
        if (!RequestParams.get(req, "id").isEmpty()) {
            Long id = idParam(req, "id");
            req.setAttribute("contract", contractService.get(user, id));
            req.setAttribute("renewal", contractService.findRenewal(user, id).orElse(null));
            render(req, resp, Views.AGENT_CONTRACT_DETAIL);
            return;
        }

        // Filtres de recherche : un filtre vide ou invalide est simplement ignoré
        ContractSearchRequest criteria = new ContractSearchRequest(
                RequestParams.get(req, "q"),
                statusParam(req, "status"),
                RequestParams.optionalLong(req, "type"));

        req.setAttribute("contracts", contractService.search(user, criteria));
        req.setAttribute("statuses", ContractStatus.values());
        req.setAttribute("insuranceTypes", insuranceTypeService.listAll(user));
        render(req, resp, Views.AGENT_CONTRACTS);
    }

    private static ContractStatus statusParam(HttpServletRequest req, String name) {
        try {
            return ContractStatus.valueOf(RequestParams.get(req, name));
        } catch (IllegalArgumentException e) {
            return null; // "" (Tous) ou valeur inconnue
        }
    }
}

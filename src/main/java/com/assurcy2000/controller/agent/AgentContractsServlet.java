package com.assurcy2000.controller.agent;

import com.assurcy2000.controller.BaseServlet;
import com.assurcy2000.dto.SessionUser;
import com.assurcy2000.dto.request.ContractSearchRequest;
import com.assurcy2000.model.enums.ContractStatus;
import com.assurcy2000.service.ContractService;
import com.assurcy2000.service.InsuranceTypeService;
import com.assurcy2000.util.Views;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * Liste + recherche des contrats.
 *   GET /agent/contrats?q=…&statut=ACTIF&type=3   agent (avec les actions)
 *   GET /admin/contrats?…                         admin, consultation seule (la JSP masque les actions)
 * Créer, modifier, suspendre, résilier, renouveler : étapes suivantes (liens déjà présents dans la JSP).
 */
@WebServlet({"/agent/contrats", "/admin/contrats"})
public class AgentContractsServlet extends BaseServlet {

    private final ContractService contractService = new ContractService();
    private final InsuranceTypeService insuranceTypeService = new InsuranceTypeService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        SessionUser user = user(req);

        // Filtres de recherche : un filtre vide ou invalide est simplement ignoré
        ContractSearchRequest criteria = new ContractSearchRequest(
                param(req, "q").trim(),
                statusParam(req, "statut"),
                optionalId(req, "type"));

        req.setAttribute("contrats", contractService.search(user, criteria));
        req.setAttribute("statuts", ContractStatus.values());
        req.setAttribute("typesAssurance", insuranceTypeService.listAll(user));
        render(req, resp, Views.AGENT_CONTRACTS);
    }

    private static ContractStatus statusParam(HttpServletRequest req, String name) {
        try {
            return ContractStatus.valueOf(param(req, name).trim());
        } catch (IllegalArgumentException e) {
            return null; // "" (Tous) ou valeur inconnue
        }
    }

    private static Long optionalId(HttpServletRequest req, String name) {
        try {
            return Long.valueOf(param(req, name).trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}

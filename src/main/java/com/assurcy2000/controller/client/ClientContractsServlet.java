package com.assurcy2000.controller.client;

import com.assurcy2000.controller.BaseServlet;
import com.assurcy2000.model.Contract;
import com.assurcy2000.model.enums.ContractStatus;
import com.assurcy2000.service.ContractService;
import com.assurcy2000.util.Views;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.List;

/**
 * GET /client/contracts                 -> mes contrats
 * GET /client/contracts?status=ACTIF    -> mes contrats filtrés par statut (onglets)
 * GET /client/contracts?id=12           -> détail (404 si ce n'est pas le mien)
 */
@WebServlet("/client/contracts")
public class ClientContractsServlet extends BaseServlet {

    private final ContractService contractService = new ContractService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (req.getParameter("id") == null) {
            List<Contract> all = contractService.listFor(user(req));
            ContractStatus status = statusParam(req);
            req.setAttribute("contracts", status == null ? all
                    : all.stream().filter(c -> c.getStatus() == status).toList());
            req.setAttribute("statuses", ContractStatus.values());
            req.setAttribute("selectedStatus", status);
            req.setAttribute("totalCount", all.size());
            render(req, resp, Views.CLIENT_CONTRACTS);
        } else {
            req.setAttribute("contract", contractService.get(user(req), idParam(req, "id")));
            render(req, resp, Views.CLIENT_CONTRACT);
        }
    }

    /** Statut demandé par l'onglet, null pour « Tous » ou une valeur inconnue. */
    private static ContractStatus statusParam(HttpServletRequest req) {
        String value = req.getParameter("status");
        if (value == null || value.isBlank()) return null;
        try {
            return ContractStatus.valueOf(value.trim());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}

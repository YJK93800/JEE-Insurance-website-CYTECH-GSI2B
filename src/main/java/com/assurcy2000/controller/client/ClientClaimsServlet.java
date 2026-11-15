package com.assurcy2000.controller.client;

import com.assurcy2000.controller.BaseServlet;
import com.assurcy2000.dto.DeclareClaimRequest;
import com.assurcy2000.model.Claim;
import com.assurcy2000.model.enums.ClaimType;
import com.assurcy2000.model.enums.ContractStatus;
import com.assurcy2000.security.SessionUser;
import com.assurcy2000.service.ClaimService;
import com.assurcy2000.service.ContractService;
import com.assurcy2000.service.exceptions.ValidationException;
import com.assurcy2000.util.Views;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * GET  /client/claims            → mes sinistres
 * GET  /client/claims?id=3       → détail d'un de mes sinistres
 * GET  /client/claims?new=1      → formulaire de déclaration
 * POST /client/claims            → déclarer un sinistre
 */
@WebServlet("/client/claims")
public class ClientClaimsServlet extends BaseServlet {

    private final ClaimService claimService = new ClaimService();
    private final ContractService contractService = new ContractService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        SessionUser user = user(req);
        if (req.getParameter("new") != null) {
            showForm(req, resp, user);
        } else if (req.getParameter("id") != null) {
            req.setAttribute("claim", claimService.get(user, idParam(req, "id")));
            render(req, resp, Views.CLIENT_CLAIM);
        } else {
            req.setAttribute("claims", claimService.listFor(user));
            render(req, resp, Views.CLIENT_CLAIMS);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        SessionUser user = user(req);
        DeclareClaimRequest form = new DeclareClaimRequest(param(req, "contractId"), param(req, "claimDate"),
                param(req, "type"), param(req, "description"), param(req, "estimatedAmount"));
        try {
            Claim claim = claimService.declare(user, form);
            redirect(req, resp, "/client/claims?id=" + claim.getId());
        } catch (ValidationException e) {
            req.setAttribute("errors", e.getErrors());
            req.setAttribute("form", form);
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            showForm(req, resp, user);
        }
    }

    private void showForm(HttpServletRequest req, HttpServletResponse resp, SessionUser user)
            throws ServletException, IOException {
        req.setAttribute("contracts", contractService.listFor(user).stream()
                .filter(c -> c.getStatus() == ContractStatus.ACTIF).toList());
        req.setAttribute("claimTypes", ClaimType.values());
        render(req, resp, Views.CLIENT_CLAIM_FORM);
    }
}

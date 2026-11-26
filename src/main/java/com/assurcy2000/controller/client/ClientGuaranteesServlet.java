package com.assurcy2000.controller.client;

import com.assurcy2000.controller.BaseServlet;
import com.assurcy2000.service.ContractService;
import com.assurcy2000.util.Views;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * GET /client/guarantees -> garanties de mes contrats en cours (actifs, suspendus, en attente), contrat par contrat.
 */
@WebServlet("/client/guarantees")
public class ClientGuaranteesServlet extends BaseServlet {

    private final ContractService contractService = new ContractService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setAttribute("contracts", contractService.listOwnWithGuarantees(user(req)));
        render(req, resp, Views.CLIENT_GUARANTEES);
    }
}

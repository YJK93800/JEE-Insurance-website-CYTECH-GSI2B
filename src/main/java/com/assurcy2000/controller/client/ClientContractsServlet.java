package com.assurcy2000.controller.client;

import com.assurcy2000.controller.BaseServlet;
import com.assurcy2000.service.ContractService;
import com.assurcy2000.util.Views;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/** GET /client/contracts → mes contrats ; GET /client/contracts?id=12 → détail (404 si ce n'est pas le mien). */
@WebServlet("/client/contracts")
public class ClientContractsServlet extends BaseServlet {

    private final ContractService contractService = new ContractService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (req.getParameter("id") == null) {
            req.setAttribute("contracts", contractService.listFor(user(req)));
            render(req, resp, Views.CLIENT_CONTRACTS);
        } else {
            req.setAttribute("contract", contractService.get(user(req), idParam(req, "id")));
            render(req, resp, Views.CLIENT_CONTRACT);
        }
    }
}

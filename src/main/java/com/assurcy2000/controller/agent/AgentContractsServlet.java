package com.assurcy2000.controller.agent;

import com.assurcy2000.controller.BaseServlet;
import com.assurcy2000.security.SessionUser;
import com.assurcy2000.service.ContractService;
import com.assurcy2000.service.UserService;
import com.assurcy2000.service.exceptions.ValidationException;
import com.assurcy2000.util.Views;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@WebServlet("/agent/contracts")
public class AgentContractsServlet extends BaseServlet {

    private final ContractService contractService = new ContractService();
    private final UserService userService = new UserService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        SessionUser user = user(req);
        if (req.getParameter("id") != null) {
            showDetail(req, resp, user, idParam(req, "id"));
            return;
        }
        if (req.getParameter("clientId") != null) {
            req.setAttribute("contracts", contractService.listByClient(user, idParam(req, "clientId")));
        } else {
            req.setAttribute("contracts", contractService.search(user, req.getParameter("q")));
        }
        req.setAttribute("clients", userService.listClients(user));
        render(req, resp, Views.AGENT_CONTRACTS);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        SessionUser user = user(req);
        Long id = idParam(req, "id");
        try {
            switch (param(req, "action")) {
                case "suspend" -> contractService.suspend(user, id);
                default -> throw new ValidationException(java.util.Map.of("action", "Action inconnue."));
            }
            redirect(req, resp, "/agent/contracts?id=" + id);
        } catch (ValidationException e) {
            req.setAttribute("errors", e.getErrors());
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            showDetail(req, resp, user, id);
        }
    }

    private void showDetail(HttpServletRequest req, HttpServletResponse resp, SessionUser user, Long id)
            throws ServletException, IOException {
        req.setAttribute("contract", contractService.get(user, id));
        render(req, resp, Views.AGENT_CONTRACT);
    }
}

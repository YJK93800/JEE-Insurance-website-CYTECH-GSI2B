package com.assurcy2000.controller.client;

import com.assurcy2000.controller.BaseServlet;
import com.assurcy2000.dto.ClientDashboard;
import com.assurcy2000.service.ClientDashboardService;
import com.assurcy2000.util.Views;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@WebServlet("/client/dashboard")
public class ClientDashboardServlet extends BaseServlet {

    private final ClientDashboardService service = new ClientDashboardService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        ClientDashboard d = service.forClient(user(req));
        req.setAttribute("contracts", d.recentContracts());
        req.setAttribute("contractCount", d.contractCount());
        req.setAttribute("activeContractCount", d.activeContractCount());
        req.setAttribute("pendingContractCount", d.pendingContractCount());
        req.setAttribute("totalPremiums", d.totalPremiums());
        req.setAttribute("openClaimCount", d.openClaimCount());
        req.setAttribute("lastClaim", d.lastClaim());
        req.setAttribute("nextPayment", d.nextPayment());
        req.setAttribute("recentPayments", d.recentPayments());
        render(req, resp, Views.CLIENT_DASHBOARD);
    }
}

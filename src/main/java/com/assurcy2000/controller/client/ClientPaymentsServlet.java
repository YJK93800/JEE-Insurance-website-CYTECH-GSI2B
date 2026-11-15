package com.assurcy2000.controller.client;

import com.assurcy2000.controller.BaseServlet;
import com.assurcy2000.service.PaymentService;
import com.assurcy2000.util.Views;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/** GET /client/payments → mes paiements ; GET /client/payments?contractId=12 → ceux d'un de mes contrats. */
@WebServlet("/client/payments")
public class ClientPaymentsServlet extends BaseServlet {

    private final PaymentService paymentService = new PaymentService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (req.getParameter("contractId") == null) {
            req.setAttribute("payments", paymentService.listFor(user(req)));
        } else {
            req.setAttribute("payments", paymentService.listByContract(user(req), idParam(req, "contractId")));
        }
        render(req, resp, Views.CLIENT_PAYMENTS);
    }
}

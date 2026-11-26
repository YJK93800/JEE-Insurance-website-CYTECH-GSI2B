package com.assurcy2000.controller.client;

import com.assurcy2000.controller.BaseServlet;
import com.assurcy2000.dto.PaymentFilter;
import com.assurcy2000.dto.SessionUser;
import com.assurcy2000.model.Payment;
import com.assurcy2000.service.PaymentService;
import com.assurcy2000.util.RequestParams;
import com.assurcy2000.util.Views;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.List;

/**
 * Paiements du client connecté (consultation seule).
 *   GET /client/payments                     historique (payés, annulés, déjà dus)
 *   GET /client/payments?filter=paid         paiements effectués
 *   GET /client/payments?filter=pending      en attente : dus à ce jour, non payés
 *   GET /client/payments?filter=due          échéances à venir
 *   &contractId=12                           limité à un de ses contrats (404 si ce n'est pas le sien)
 */
@WebServlet("/client/payments")
public class ClientPaymentsServlet extends BaseServlet {

    private final PaymentService paymentService = new PaymentService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        SessionUser user = user(req);
        Long contractId = RequestParams.get(req, "contractId").isEmpty() ? null : idParam(req, "contractId");
        PaymentFilter filter = PaymentFilter.fromParam(RequestParams.get(req, "filter"));

        List<Payment> all = paymentService.visible(user, contractId);
        req.setAttribute("payments", paymentService.filter(all, filter, null));
        req.setAttribute("summary", paymentService.summary(all));
        req.setAttribute("filters", PaymentFilter.values());
        req.setAttribute("filter", filter);
        req.setAttribute("contractId", contractId);
        req.setAttribute("today", paymentService.today());
        render(req, resp, Views.CLIENT_PAYMENTS);
    }
}

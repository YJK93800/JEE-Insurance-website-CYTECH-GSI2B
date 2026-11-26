package com.assurcy2000.controller.agent;

import com.assurcy2000.controller.BaseServlet;
import com.assurcy2000.dto.PaymentFilter;
import com.assurcy2000.dto.SessionUser;
import com.assurcy2000.model.Payment;
import com.assurcy2000.model.enums.PaymentMethod;
import com.assurcy2000.security.Permission;
import com.assurcy2000.security.Policy;
import com.assurcy2000.security.UserSession;
import com.assurcy2000.service.PaymentSchedule;
import com.assurcy2000.service.PaymentService;
import com.assurcy2000.service.exceptions.NotFoundException;
import com.assurcy2000.service.exceptions.ValidationException;
import com.assurcy2000.util.FormView;
import com.assurcy2000.util.RequestParams;
import com.assurcy2000.util.Views;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Paiements de tous les clients.
 *   GET  /agent/payments[?filter=paid|pending|due][&q=…][&contractId=12][&pay=ID]
 *        liste avec onglets et recherche ; pay=ID pré-sélectionne une échéance dans le formulaire
 *   POST /agent/payments  paymentId, paymentMethod, paymentDate -> l'échéance devient EFFECTUE
 *   GET  /admin/payments[…] admin : mêmes listes, consultation seule (pas de formulaire)
 */
@WebServlet({"/agent/payments", "/admin/payments"})
public class AgentPaymentsServlet extends BaseServlet {

    private final PaymentService paymentService = new PaymentService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        show(req, resp, user(req), Map.of());
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        SessionUser user = user(req);

        // 1. Vérification des saisies par la servlet
        Map<String, String> errors = new LinkedHashMap<>();
        Long paymentId = RequestParams.optionalLong(req, "paymentId");
        if (paymentId == null) errors.put("paymentId", "Choisissez l'échéance à encaisser.");

        PaymentMethod method = null;
        try {
            method = PaymentMethod.valueOf(RequestParams.get(req, "paymentMethod"));
        } catch (IllegalArgumentException e) {
            errors.put("paymentMethod", "Choisissez le mode de paiement.");
        }

        LocalDate date = null;
        String dateText = RequestParams.get(req, "paymentDate");
        if (dateText.isEmpty()) {
            errors.put("paymentDate", "La date de paiement est obligatoire.");
        } else {
            try {
                date = LocalDate.parse(dateText);
            } catch (DateTimeParseException e) {
                errors.put("paymentDate", "La date de paiement n'est pas valide.");
            }
        }

        // 2. Le service vérifie l'échéance (ni payée ni annulée) et la date (pas dans le futur)
        if (errors.isEmpty()) {
            try {
                Payment p = paymentService.markPaid(user, paymentId, method, date);
                UserSession.flash(req, "Paiement de l'échéance du " + frDate(p.getDueDate()) + " enregistré pour "
                        + p.getContract().getNumber() + ".", "success");
                redirect(req, resp, "/agent/payments?filter=paid");
                return;
            } catch (ValidationException e) {
                errors.putAll(e.getErrors());
            }
        }
        resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        show(req, resp, user, errors);
    }

    private void show(HttpServletRequest req, HttpServletResponse resp, SessionUser user, Map<String, String> errors)
            throws ServletException, IOException {
        Long contractId = RequestParams.get(req, "contractId").isEmpty() ? null : idParam(req, "contractId");
        PaymentFilter filter = PaymentFilter.fromParam(RequestParams.get(req, "filter"));
        String q = RequestParams.get(req, "q");

        List<Payment> all = paymentService.visible(user, contractId);
        req.setAttribute("payments", paymentService.filter(all, filter, q));
        req.setAttribute("summary", paymentService.summary(all));
        req.setAttribute("filters", PaymentFilter.values());
        req.setAttribute("filter", filter);
        req.setAttribute("contractId", contractId);
        req.setAttribute("today", paymentService.today());
        req.setAttribute(FormView.ERRORS, errors);

        boolean canWrite = Policy.can(user, Permission.PAYMENT_WRITE);
        req.setAttribute("canWrite", canWrite);
        if (canWrite) {
            List<Payment> payable = new ArrayList<>(paymentService.payable(user));
            // Échéance pré-sélectionnée : celle du POST en erreur, sinon ?pay=ID (bouton « Encaisser » d'une ligne).
            // Toujours proposée si elle est à payer, même due dans plus d'un mois.
            Long selected = RequestParams.optionalLong(req, "paymentId");
            if (selected == null) selected = RequestParams.optionalLong(req, "pay");
            Long wanted = selected;
            if (wanted != null && payable.stream().noneMatch(p -> p.getId().equals(wanted))) {
                try {
                    Payment p = paymentService.get(user, wanted);
                    if (PaymentSchedule.isUnpaid(p)) payable.add(0, p);
                } catch (NotFoundException ignored) {
                    // id inconnu : rien à pré-sélectionner
                }
            }
            req.setAttribute("payable", payable);
            req.setAttribute("selectedPaymentId", selected);
            req.setAttribute("paymentMethods", PaymentMethod.values());
        }
        render(req, resp, Views.AGENT_PAYMENTS);
    }

    private static String frDate(LocalDate d) {
        return String.format("%02d/%02d/%d", d.getDayOfMonth(), d.getMonthValue(), d.getYear());
    }
}

package com.assurcy2000.controller.agent;

import com.assurcy2000.controller.BaseServlet;
import com.assurcy2000.dto.SessionUser;
import com.assurcy2000.model.Contract;
import com.assurcy2000.security.UserSession;
import com.assurcy2000.service.ContractService;
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
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Actions de statut sur un contrat (boutons de agent/contracts.jsp).
 *   POST /agent/contracts/action      contractId + action = activate | suspend | renew  -> retour à la liste + message
 *   GET  /agent/contracts/terminate?id=12   page de confirmation (date + motif)
 *   POST /agent/contracts/terminate         résiliation
 * Les règles (quels statuts autorisent quelle action) sont dans ContractService.
 */
@WebServlet({"/agent/contracts/action", "/agent/contracts/terminate"})
public class ContractActionServlet extends BaseServlet {

    private static final int REASON_MAX = 255; // contrat.motif_resiliation VARCHAR(255)

    private final ContractService contractService = new ContractService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (!isTerminatePage(req)) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        Contract contract = contractService.get(user(req), idParam(req, "id"));
        if (!ContractService.isTerminable(contract)) {
            UserSession.flash(req, "Le contrat " + contract.getNumber() + " est déjà résilié ou expiré.", "error");
            redirect(req, resp, "/agent/contracts");
            return;
        }
        showTerminateForm(req, resp, contract, Map.of());
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (isTerminatePage(req)) {
            terminate(req, resp);
            return;
        }

        SessionUser user = user(req);
        Long id = idParam(req, "contractId");
        String action = RequestParams.get(req, "action");
        try {
            switch (action) {
                case "activate" -> {
                    Contract c = contractService.activate(user, id);
                    UserSession.flash(req, "Contrat " + c.getNumber() + " activé.", "success");
                }
                case "suspend" -> {
                    Contract c = contractService.suspend(user, id);
                    UserSession.flash(req, "Contrat " + c.getNumber() + " suspendu.", "success");
                }
                case "renew" -> {
                    Contract c = contractService.renew(user, id);
                    UserSession.flash(req, "Contrat " + c.getNumber() + " créé en renouvellement de "
                            + c.getPreviousContract().getNumber() + " (en attente). Vérifiez la prime avant de l'activer.", "success");
                    redirect(req, resp, "/agent/contracts/edit?id=" + c.getId()); // l'agent ajuste prime / garanties
                    return;
                }
                default -> {
                    resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Action inconnue.");
                    return;
                }
            }
        } catch (ValidationException e) {
            UserSession.flash(req, String.join(" ", e.getErrors().values()), "error");
        }
        redirect(req, resp, "/agent/contracts");
    }

    private void terminate(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        SessionUser user = user(req);
        Long id = idParam(req, "id");

        // 1. Vérification des saisies par la servlet
        Map<String, String> errors = new LinkedHashMap<>();
        LocalDate date = null;
        String dateText = RequestParams.get(req, "terminationDate");
        if (dateText.isEmpty()) {
            errors.put("terminationDate", "La date de résiliation est obligatoire.");
        } else {
            try {
                date = LocalDate.parse(dateText);
            } catch (DateTimeParseException e) {
                errors.put("terminationDate", "La date de résiliation n'est pas valide.");
            }
        }
        String reason = RequestParams.get(req, "terminationReason");
        if (reason.isEmpty()) errors.put("terminationReason", "Le motif est obligatoire.");
        else if (reason.length() > REASON_MAX) errors.put("terminationReason", "Le motif est trop long (" + REASON_MAX + " caractères max).");

        // 2. Le service vérifie le statut et la période du contrat
        if (errors.isEmpty()) {
            try {
                Contract c = contractService.terminate(user, id, date, reason);
                UserSession.flash(req, "Contrat " + c.getNumber() + " résilié.", "success");
                redirect(req, resp, "/agent/contracts");
                return;
            } catch (ValidationException e) {
                errors.putAll(e.getErrors());
            }
        }

        resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        showTerminateForm(req, resp, contractService.get(user, id), errors);
    }

    /** Page de confirmation : date proposée = aujourd'hui, ramenée dans la période du contrat. */
    private void showTerminateForm(HttpServletRequest req, HttpServletResponse resp, Contract contract,
                                   Map<String, String> errors) throws ServletException, IOException {
        LocalDate suggested = LocalDate.now();
        if (suggested.isBefore(contract.getStartDate())) suggested = contract.getStartDate();
        if (suggested.isAfter(contract.getEndDate())) suggested = contract.getEndDate();
        req.setAttribute("contract", contract);
        req.setAttribute("defaultTerminationDate", suggested);
        req.setAttribute(FormView.ERRORS, errors);
        render(req, resp, Views.AGENT_CONTRACT_TERMINATE);
    }

    private static boolean isTerminatePage(HttpServletRequest req) {
        return "/agent/contracts/terminate".equals(req.getServletPath());
    }
}

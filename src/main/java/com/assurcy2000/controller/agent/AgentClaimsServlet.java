package com.assurcy2000.controller.agent;

import com.assurcy2000.controller.BaseServlet;
import com.assurcy2000.dto.SessionUser;
import com.assurcy2000.model.ClaimAttachment;
import com.assurcy2000.model.enums.ClaimStatus;
import com.assurcy2000.service.ClaimService;
import com.assurcy2000.service.exceptions.ValidationException;
import com.assurcy2000.util.FormView;
import com.assurcy2000.util.RequestParams;
import com.assurcy2000.util.Validation;
import com.assurcy2000.util.Views;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

@WebServlet({"/agent/claims", "/admin/claims"})
public class AgentClaimsServlet extends BaseServlet {

    private final ClaimService claimService = new ClaimService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        SessionUser user = user(req);
        if (req.getParameter("attachment") != null) {
            ClaimAttachment attachment = claimService.attachment(user, idParam(req, "attachment"));
            sendAttachment(resp, attachment, claimService.fileOf(attachment));
        } else if (req.getParameter("id") != null) {
            showDetail(req, resp, user, idParam(req, "id"));
        } else {
            req.setAttribute("claims", claimService.listFor(user));
            render(req, resp, Views.AGENT_CLAIMS);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        SessionUser user = user(req);
        Long id = idParam(req, "id");
        String action = RequestParams.get(req, "action");

        // 1. Vérification des saisies par la servlet
        Map<String, String> errors = new LinkedHashMap<>();
        ClaimStatus status = null;
        BigDecimal amount = null;
        String content = null;
        switch (action) {
            case "status" -> {
                try {
                    status = ClaimStatus.valueOf(RequestParams.get(req, "status"));
                } catch (IllegalArgumentException e) {
                    errors.put("status", "Choisissez le nouveau statut.");
                }
            }
            case "indemnify" -> amount = Validation.amount(RequestParams.get(req, "amount"), "amount", "Le montant",
                    Validation.CLAIM_AMOUNT_MAX, true, errors);
            case "comment" -> content = Validation.comment(RequestParams.get(req, "content"), errors);
            default -> errors.put("action", "Action inconnue.");
        }

        // 2. Le service vérifie les droits et les règles du dossier (transitions, sinistre accepté avant indemnisation)
        if (errors.isEmpty()) {
            try {
                switch (action) {
                    case "status" -> claimService.changeStatus(user, id, status);
                    case "indemnify" -> claimService.indemnify(user, id, amount.toPlainString());
                    default -> {
                        claimService.addComment(user, id, content);
                        redirect(req, resp, "/agent/claims?id=" + id + "#comments");
                        return;
                    }
                }
                redirect(req, resp, "/agent/claims?id=" + id);
                return;
            } catch (ValidationException e) {
                errors.putAll(e.getErrors());
            }
        }
        req.setAttribute(FormView.ERRORS, errors);
        resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        showDetail(req, resp, user, id);
    }

    private void showDetail(HttpServletRequest req, HttpServletResponse resp, SessionUser user, Long id)
            throws ServletException, IOException {
        req.setAttribute("claim", claimService.get(user, id));
        req.setAttribute("history", claimService.history(user, id));
        req.setAttribute("comments", claimService.comments(user, id));
        req.setAttribute("attachments", claimService.attachments(user, id));
        render(req, resp, Views.AGENT_CLAIM);
    }
}

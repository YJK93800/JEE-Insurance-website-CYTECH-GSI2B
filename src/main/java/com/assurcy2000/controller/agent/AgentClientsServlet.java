package com.assurcy2000.controller.agent;

import com.assurcy2000.controller.BaseServlet;
import com.assurcy2000.dto.SessionUser;
import com.assurcy2000.model.User;
import com.assurcy2000.security.UserSession;
import com.assurcy2000.service.ClientService;
import com.assurcy2000.service.UserService;
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
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Consultation des clients (agent/clients.jsp).
 *   GET  /agent/clients?q=texte          liste filtrée par nom, email ou téléphone
 *   GET  /agent/clients?q=texte&id=5     liste + fiche du client 5 (contrats et sinistres) à droite
 *   POST /agent/clients  action=name, id, firstName, lastName, q   correction du nom du client
 * Les contrats se créent par ContractFormServlet (bouton de la fiche).
 */
@WebServlet("/agent/clients")
public class AgentClientsServlet extends BaseServlet {

    private final ClientService clientService = new ClientService();
    private final UserService userService = new UserService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        showPage(req, resp, user(req));
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        SessionUser user = user(req);
        Long id = idParam(req, "id");
        if (!"name".equals(RequestParams.get(req, "action"))) {
            showWithErrors(req, resp, user, Map.of("action", "Action inconnue."));
            return;
        }

        // 1. Vérification des saisies par la servlet
        Map<String, String> errors = new LinkedHashMap<>();
        String firstName = Validation.firstName(RequestParams.get(req, "firstName"), errors);
        String lastName = Validation.lastName(RequestParams.get(req, "lastName"), errors);

        // 2. Le service vérifie les droits (agent : clients seulement, pas soi-même)
        if (errors.isEmpty()) {
            try {
                User updated = userService.updateName(user, id, firstName, lastName);
                UserSession.flash(req, "Nom corrigé : " + updated.getFirstName() + " " + updated.getLastName() + ".", "success");
                String q = RequestParams.get(req, "q");
                redirect(req, resp, "/agent/clients?id=" + id
                        + (q.isEmpty() ? "" : "&q=" + URLEncoder.encode(q, StandardCharsets.UTF_8)));
                return;
            } catch (ValidationException e) {
                errors.putAll(e.getErrors());
            }
        }
        showWithErrors(req, resp, user, errors);
    }

    private void showWithErrors(HttpServletRequest req, HttpServletResponse resp, SessionUser user,
                                Map<String, String> errors) throws ServletException, IOException {
        req.setAttribute(FormView.ERRORS, errors);
        resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        showPage(req, resp, user); // la saisie est réaffichée via param.firstName / param.lastName
    }

    private void showPage(HttpServletRequest req, HttpServletResponse resp, SessionUser user)
            throws ServletException, IOException {
        req.setAttribute("clients", clientService.search(user, RequestParams.get(req, "q")));

        // ?id=5 : fiche du client (404 si l'id n'est pas celui d'un client)
        if (!RequestParams.get(req, "id").isBlank()) {
            req.setAttribute("selection", clientService.getDetail(user, idParam(req, "id")));
        }
        render(req, resp, Views.AGENT_CLIENTS);
    }
}

package com.assurcy2000.controller.agent;

import com.assurcy2000.controller.BaseServlet;
import com.assurcy2000.dto.SessionUser;
import com.assurcy2000.service.ClientService;
import com.assurcy2000.util.RequestParams;
import com.assurcy2000.util.Views;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * Consultation des clients (agent/clients.jsp).
 *   GET /agent/clients?q=texte          liste filtrée par nom, email ou téléphone
 *   GET /agent/clients?q=texte&id=5     liste + fiche du client 5 (contrats et sinistres) à droite
 * Lecture seule : les contrats se créent par ContractFormServlet (bouton de la fiche).
 */
@WebServlet("/agent/clients")
public class AgentClientsServlet extends BaseServlet {

    private final ClientService clientService = new ClientService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        SessionUser user = user(req);

        req.setAttribute("clients", clientService.search(user, RequestParams.get(req, "q")));

        // ?id=5 : fiche du client (404 si l'id n'est pas celui d'un client)
        if (!RequestParams.get(req, "id").isBlank()) {
            req.setAttribute("selection", clientService.getDetail(user, idParam(req, "id")));
        }
        render(req, resp, Views.AGENT_CLIENTS);
    }
}

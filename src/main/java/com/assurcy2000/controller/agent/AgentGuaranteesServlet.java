package com.assurcy2000.controller.agent;

import com.assurcy2000.controller.BaseServlet;
import com.assurcy2000.dto.SessionUser;
import com.assurcy2000.dto.request.GuaranteeRequest;
import com.assurcy2000.model.Guarantee;
import com.assurcy2000.security.UserSession;
import com.assurcy2000.service.GuaranteeService;
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

/**
 * Catalogue des garanties, géré par l'agent (section 2.4).
 *   GET  /agent/guarantees[?id=4]   liste + formulaire (création, ou modification de la garantie 4)
 *   POST /agent/guarantees          action=save   : id vide = création ; name, description, coverageLimit, deductible
 *                                   action=delete : id -> refusé si un contrat utilise la garantie
 * L'association garantie <-> contrat se fait dans le formulaire de contrat (ContractFormServlet).
 */
@WebServlet("/agent/guarantees")
public class AgentGuaranteesServlet extends BaseServlet {

    /** Montant maximal accepté pour le plafond et la franchise (colonnes DECIMAL(12,2)). */
    static final BigDecimal AMOUNT_MAX = new BigDecimal("100000000");

    private final GuaranteeService guaranteeService = new GuaranteeService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        SessionUser user = user(req);
        Long id = RequestParams.get(req, "id").isEmpty() ? null : idParam(req, "id"); // ?id=abc -> 404
        show(req, resp, user, id, Map.of());
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        SessionUser user = user(req);
        if ("delete".equals(RequestParams.get(req, "action"))) {
            delete(req, resp, user);
        } else {
            save(req, resp, user);
        }
    }

    private void save(HttpServletRequest req, HttpServletResponse resp, SessionUser user)
            throws ServletException, IOException {
        Long id = RequestParams.get(req, "id").isEmpty() ? null : idParam(req, "id");

        // 1. Vérification des saisies par la servlet
        Map<String, String> errors = new LinkedHashMap<>();
        String name = Validation.guaranteeName(RequestParams.get(req, "name"), errors);
        String description = Validation.description(RequestParams.get(req, "description"), errors);
        BigDecimal coverageLimit = Validation.amount(RequestParams.get(req, "coverageLimit"),
                "coverageLimit", "Le plafond", AMOUNT_MAX, false, errors);
        BigDecimal deductible = Validation.amount(RequestParams.get(req, "deductible"),
                "deductible", "La franchise", AMOUNT_MAX, false, errors);

        // 2. Le service vérifie le nom unique et franchise <= plafond
        if (errors.isEmpty()) {
            GuaranteeRequest form = new GuaranteeRequest(name, description, coverageLimit, deductible);
            try {
                Guarantee g = id == null
                        ? guaranteeService.create(user, form)
                        : guaranteeService.update(user, id, form);
                UserSession.flash(req, "Garantie « " + g.getName() + " » " + (id == null ? "créée." : "enregistrée."),
                        "success");
                redirect(req, resp, "/agent/guarantees");
                return;
            } catch (ValidationException e) {
                errors.putAll(e.getErrors());
            }
        }
        resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        show(req, resp, user, id, errors);
    }

    private void delete(HttpServletRequest req, HttpServletResponse resp, SessionUser user) throws IOException {
        Long id = idParam(req, "id");
        try {
            String name = guaranteeService.delete(user, id);
            UserSession.flash(req, "Garantie « " + name + " » supprimée.", "success");
        } catch (ValidationException e) {
            UserSession.flash(req, e.getErrors().values().iterator().next(), "error");
        }
        redirect(req, resp, "/agent/guarantees");
    }

    /** id : garantie en cours de modification (null = formulaire de création). */
    private void show(HttpServletRequest req, HttpServletResponse resp, SessionUser user, Long id,
                      Map<String, String> errors) throws ServletException, IOException {
        req.setAttribute("guarantees", guaranteeService.listForAgent(user));
        if (id != null) req.setAttribute("guarantee", guaranteeService.get(user, id)); // 404 si inconnue
        req.setAttribute(FormView.ERRORS, errors);
        render(req, resp, Views.AGENT_GUARANTEES);
    }
}

package com.assurcy2000.controller.admin;

import com.assurcy2000.controller.BaseServlet;
import com.assurcy2000.dto.SessionUser;
import com.assurcy2000.dto.request.InsuranceTypeRequest;
import com.assurcy2000.model.InsuranceType;
import com.assurcy2000.security.UserSession;
import com.assurcy2000.service.InsuranceTypeService;
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
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Types d'assurance, gérés par l'admin (section 2.3).
 *   GET  /admin/types[?id=2]  liste + formulaire (création, ou modification du type 2)
 *   POST /admin/types         action=save   : id vide = création ; name, description, basePrice, guaranteeIds (plusieurs)
 *                             action=active : id, value=true|false -> activer / désactiver
 *                             action=delete : id -> refusé si des contrats utilisent ce type
 */
@WebServlet("/admin/types")
public class AdminTypesServlet extends BaseServlet {

    /** Prix de base mensuel maximal accepté (type_assurance.prix_base DECIMAL(10,2)). */
    static final BigDecimal PRICE_MAX = new BigDecimal("100000");

    private final InsuranceTypeService typeService = new InsuranceTypeService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        SessionUser user = user(req);
        Long id = RequestParams.get(req, "id").isEmpty() ? null : idParam(req, "id"); // ?id=abc -> 404
        show(req, resp, user, id, Map.of());
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        SessionUser user = user(req);
        switch (RequestParams.get(req, "action")) {
            case "active" -> setActive(req, resp, user);
            case "delete" -> delete(req, resp, user);
            default -> save(req, resp, user);
        }
    }

    private void save(HttpServletRequest req, HttpServletResponse resp, SessionUser user)
            throws ServletException, IOException {
        Long id = RequestParams.get(req, "id").isEmpty() ? null : idParam(req, "id");

        // 1. Vérification des saisies par la servlet
        Map<String, String> errors = new LinkedHashMap<>();
        String name = Validation.insuranceTypeName(RequestParams.get(req, "name"), errors);
        String description = Validation.description(RequestParams.get(req, "description"), errors);
        BigDecimal basePrice = Validation.amount(RequestParams.get(req, "basePrice"),
                "basePrice", "Le prix de base", PRICE_MAX, true, errors);
        Set<Long> guaranteeIds = guaranteeIds(req, errors);

        // 2. Le service vérifie le nom unique et que les garanties existent
        if (errors.isEmpty()) {
            InsuranceTypeRequest form = new InsuranceTypeRequest(name, description, basePrice, guaranteeIds);
            try {
                InsuranceType t = id == null ? typeService.create(user, form) : typeService.update(user, id, form);
                UserSession.flash(req, "Type « " + t.getName() + " » " + (id == null ? "créé." : "enregistré."), "success");
                redirect(req, resp, "/admin/types");
                return;
            } catch (ValidationException e) {
                errors.putAll(e.getErrors());
            }
        }
        resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        show(req, resp, user, id, errors);
    }

    private void setActive(HttpServletRequest req, HttpServletResponse resp, SessionUser user) throws IOException {
        boolean active = Boolean.parseBoolean(RequestParams.get(req, "value"));
        InsuranceType t = typeService.setActive(user, idParam(req, "id"), active);
        UserSession.flash(req, "Type « " + t.getName() + " » " + (active ? "activé." : "désactivé : il n'est plus proposé pour un nouveau contrat."),
                "success");
        redirect(req, resp, "/admin/types");
    }

    private void delete(HttpServletRequest req, HttpServletResponse resp, SessionUser user) throws IOException {
        try {
            String name = typeService.delete(user, idParam(req, "id"));
            UserSession.flash(req, "Type « " + name + " » supprimé.", "success");
        } catch (ValidationException e) {
            UserSession.flash(req, e.getErrors().values().iterator().next(), "error");
        }
        redirect(req, resp, "/admin/types");
    }

    /** Cases « guaranteeIds » cochées ; une valeur qui n'est pas un nombre est une erreur. */
    private static Set<Long> guaranteeIds(HttpServletRequest req, Map<String, String> errors) {
        Set<Long> ids = new HashSet<>();
        String[] values = req.getParameterValues("guaranteeIds");
        if (values == null) return ids;
        for (String v : values) {
            try {
                ids.add(Long.valueOf(v.trim()));
            } catch (NumberFormatException e) {
                errors.put("guaranteeIds", "Garantie invalide.");
            }
        }
        return ids;
    }

    /** id : type en cours de modification (null = formulaire de création). */
    private void show(HttpServletRequest req, HttpServletResponse resp, SessionUser user, Long id,
                      Map<String, String> errors) throws ServletException, IOException {
        req.setAttribute("types", typeService.listForAdmin(user));
        req.setAttribute("guarantees", typeService.guaranteeChoices(user));
        if (id != null) req.setAttribute("type", typeService.get(user, id)); // 404 si inconnu
        req.setAttribute(FormView.ERRORS, errors);
        render(req, resp, Views.ADMIN_TYPES);
    }
}

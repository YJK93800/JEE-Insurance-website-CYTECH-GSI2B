package com.assurcy2000.controller.agent;

import com.assurcy2000.controller.BaseServlet;
import com.assurcy2000.dto.SessionUser;
import com.assurcy2000.dto.request.CreateContractRequest;
import com.assurcy2000.model.Contract;
import com.assurcy2000.security.UserSession;
import com.assurcy2000.service.ContractService;
import com.assurcy2000.service.GuaranteeService;
import com.assurcy2000.service.InsuranceTypeService;
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
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Formulaire de contrat (agent/contract-form.jsp).
 *   GET  /agent/contrats/edit   -> formulaire de création
 *   POST /agent/contrats/edit   -> création (id vide)
 * La modification (?id=12) viendra à l'étape suivante.
 */
@WebServlet("/agent/contrats/edit")
public class ContractFormServlet extends BaseServlet {

    private static final BigDecimal PREMIUM_MAX = new BigDecimal("99999999.99"); // DECIMAL(10,2)

    private final ContractService contractService = new ContractService();
    private final UserService userService = new UserService();
    private final InsuranceTypeService insuranceTypeService = new InsuranceTypeService();
    private final GuaranteeService guaranteeService = new GuaranteeService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (!RequestParams.get(req, "id").isBlank()) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND, "La modification d'un contrat n'est pas encore disponible.");
            return;
        }
        showForm(req, resp, Map.of());
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        SessionUser user = user(req);

        // 1. Vérification des saisies par la servlet (consigne, section 4)
        Map<String, String> errors = new LinkedHashMap<>();
        CreateContractRequest form = readForm(req, errors);

        // 2. Le service vérifie ce qui demande la base (client, type, garanties du type)
        if (errors.isEmpty()) {
            try {
                Contract created = contractService.create(user, form);
                UserSession.flash(req, "Contrat " + created.getNumber() + " créé (en attente).", "success");
                redirect(req, resp, "/agent/contrats");
                return;
            } catch (ValidationException e) {
                errors.putAll(e.getErrors());
            }
        }

        resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        showForm(req, resp, errors); // la JSP réaffiche la saisie via param.xxx
    }

    private CreateContractRequest readForm(HttpServletRequest req, Map<String, String> errors) {
        Long clientId = requiredId(req, "clientId", "Choisissez un client.", errors);
        Long typeId = requiredId(req, "typeAssuranceId", "Choisissez un type d'assurance.", errors);

        LocalDate start = date(req, "dateDebut", "La date de début", errors);
        LocalDate end = date(req, "dateFin", "La date de fin", errors);
        if (start != null && end != null && !end.isAfter(start)) {
            errors.put("dateFin", "La date de fin doit être après la date de début.");
        }

        // facultative : vide = prix de base du type (décidé par le service)
        BigDecimal premium = Validation.amount(RequestParams.get(req, "prime"), "prime", "La prime",
                PREMIUM_MAX, false, errors);

        Set<Long> guaranteeIds = new LinkedHashSet<>();
        String[] values = req.getParameterValues("garantieIds");
        if (values != null) {
            for (String v : values) {
                try {
                    guaranteeIds.add(Long.valueOf(v.trim()));
                } catch (NumberFormatException e) {
                    errors.put("garanties", "Garantie invalide.");
                }
            }
        }
        if (guaranteeIds.isEmpty()) errors.putIfAbsent("garanties", "Choisissez au moins une garantie.");

        return new CreateContractRequest(clientId, typeId, start, end, premium, guaranteeIds);
    }

    /** Affiche le formulaire avec les listes de choix et, en création, les dates par défaut (début aujourd'hui, fin +1 an). */
    private void showForm(HttpServletRequest req, HttpServletResponse resp, Map<String, String> errors)
            throws ServletException, IOException {
        SessionUser user = user(req);
        req.setAttribute("clients", userService.listClients(user));
        req.setAttribute("typesAssurance", insuranceTypeService.listActive(user));
        req.setAttribute("garanties", guaranteeService.listActive(user));
        LocalDate today = LocalDate.now();
        req.setAttribute("dateDebutDefaut", today);
        req.setAttribute("dateFinDefaut", today.plusYears(1).minusDays(1));
        req.setAttribute(FormView.ERRORS, errors);
        render(req, resp, Views.AGENT_CONTRACT);
    }

    private static Long requiredId(HttpServletRequest req, String name, String message, Map<String, String> errors) {
        Long id = RequestParams.optionalLong(req, name);
        if (id == null) errors.put(name, message);
        return id;
    }

    private static LocalDate date(HttpServletRequest req, String name, String label, Map<String, String> errors) {
        String value = RequestParams.get(req, name);
        if (value.isEmpty()) {
            errors.put(name, label + " est obligatoire.");
            return null;
        }
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException e) {
            errors.put(name, label + " n'est pas valide.");
            return null;
        }
    }
}

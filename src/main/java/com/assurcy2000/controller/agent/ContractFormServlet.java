package com.assurcy2000.controller.agent;

import com.assurcy2000.controller.BaseServlet;
import com.assurcy2000.dto.SessionUser;
import com.assurcy2000.dto.request.CreateContractRequest;
import com.assurcy2000.model.Contract;
import com.assurcy2000.model.enums.ContractStatus;
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
 * Formulaire de contrat (agent/contract-form.jsp), création et modification.
 *   GET  /agent/contracts/edit          -> formulaire de création
 *   GET  /agent/contracts/edit?id=12    -> formulaire de modification, pré-rempli
 *   POST /agent/contracts/edit          -> création si id vide, modification sinon
 * Le statut ne se change pas ici, mais par les actions de la liste (activer, suspendre…).
 */
@WebServlet("/agent/contracts/edit")
public class ContractFormServlet extends BaseServlet {

    private static final BigDecimal PREMIUM_MAX = new BigDecimal("99999999.99"); // DECIMAL(10,2)

    private final ContractService contractService = new ContractService();
    private final UserService userService = new UserService();
    private final InsuranceTypeService insuranceTypeService = new InsuranceTypeService();
    private final GuaranteeService guaranteeService = new GuaranteeService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Long id = contractId(req);
        Contract contract = id == null ? null : contractService.get(user(req), id); // 404 si introuvable
        showForm(req, resp, Map.of(), contract);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        SessionUser user = user(req);
        Long id = contractId(req);
        Contract existing = id == null ? null : contractService.get(user, id); // 404 si introuvable

        // 1. Vérification des saisies par la servlet (consigne, section 4)
        Map<String, String> errors = new LinkedHashMap<>();
        CreateContractRequest form = readForm(req, errors);

        // 2. Le service vérifie ce qui demande la base (client, type, garanties du type, règles de modification)
        if (errors.isEmpty()) {
            try {
                if (existing == null) {
                    Contract created = contractService.create(user, form);
                    UserSession.flash(req, "Contrat " + created.getNumber() + " créé (en attente).", "success");
                } else {
                    Contract updated = contractService.update(user, id, form);
                    UserSession.flash(req, "Contrat " + updated.getNumber() + " modifié.", "success");
                }
                redirect(req, resp, "/agent/contracts");
                return;
            } catch (ValidationException e) {
                errors.putAll(e.getErrors());
            }
        }

        resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        showForm(req, resp, errors, existing); // la JSP réaffiche la saisie via param.xxx
    }

    /** id du contrat à modifier, null en création. Un id présent mais invalide donne une 404. */
    private Long contractId(HttpServletRequest req) {
        return RequestParams.get(req, "id").isBlank() ? null : idParam(req, "id");
    }

    private CreateContractRequest readForm(HttpServletRequest req, Map<String, String> errors) {
        Long clientId = requiredId(req, "clientId", "Choisissez un client.", errors);
        Long typeId = requiredId(req, "insuranceTypeId", "Choisissez un type d'assurance.", errors);

        LocalDate start = date(req, "startDate", "La date de début", errors);
        LocalDate end = date(req, "endDate", "La date de fin", errors);
        if (start != null && end != null && !end.isAfter(start)) {
            errors.put("endDate", "La date de fin doit être après la date de début.");
        }

        // facultative : vide = prix de base du type (décidé par le service)
        BigDecimal premium = Validation.amount(RequestParams.get(req, "premium"), "premium", "La prime",
                PREMIUM_MAX, false, errors);

        Set<Long> guaranteeIds = new LinkedHashSet<>();
        String[] values = req.getParameterValues("guaranteeIds");
        if (values != null) {
            for (String v : values) {
                try {
                    guaranteeIds.add(Long.valueOf(v.trim()));
                } catch (NumberFormatException e) {
                    errors.put("guaranteeIds", "Garantie invalide.");
                }
            }
        }
        if (guaranteeIds.isEmpty()) errors.putIfAbsent("guaranteeIds", "Choisissez au moins une garantie.");

        return new CreateContractRequest(clientId, typeId, start, end, premium, guaranteeIds);
    }

    /**
     * Affiche le formulaire avec les listes de choix.
     * Création (contract null) : dates par défaut (début aujourd'hui, fin +1 an).
     * Modification : le contrat pré-remplit les champs ; client et type sont verrouillés s'il n'est plus EN_ATTENTE.
     */
    private void showForm(HttpServletRequest req, HttpServletResponse resp, Map<String, String> errors,
                          Contract contract) throws ServletException, IOException {
        SessionUser user = user(req);
        req.setAttribute("contract", contract);
        req.setAttribute("identityLocked", contract != null && contract.getStatus() != ContractStatus.EN_ATTENTE);
        req.setAttribute("clients", userService.listClients(user));
        req.setAttribute("insuranceTypes", insuranceTypeService.listActive(user));
        req.setAttribute("guarantees", guaranteeService.listActive(user));
        LocalDate today = LocalDate.now();
        req.setAttribute("defaultStartDate", today);
        req.setAttribute("defaultEndDate", today.plusYears(1).minusDays(1));
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

package com.assurcy2000.controller.client;

import com.assurcy2000.controller.BaseServlet;
import com.assurcy2000.dto.request.DeclareClaimRequest;
import com.assurcy2000.dto.request.UploadedFile;
import com.assurcy2000.model.Claim;
import com.assurcy2000.model.ClaimAttachment;
import com.assurcy2000.model.enums.ClaimType;
import com.assurcy2000.model.enums.ContractStatus;
import com.assurcy2000.dto.SessionUser;
import com.assurcy2000.security.UserSession;
import com.assurcy2000.service.ClaimService;
import com.assurcy2000.service.ContractService;
import com.assurcy2000.service.exceptions.ValidationException;
import com.assurcy2000.util.FormView;
import com.assurcy2000.util.RequestParams;
import com.assurcy2000.util.Validation;
import com.assurcy2000.util.Views;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;

/**
 * GET  /client/claims                → mes sinistres
 * GET  /client/claims?id=3           → détail d'un de mes sinistres (historique, pièces justificatives)
 * GET  /client/claims?new=1          → formulaire de déclaration
 * GET  /client/claims?attachment=7   → télécharger une de mes pièces justificatives
 * POST /client/claims                → déclarer un sinistre (multipart, champ fichiers "attachments")
 * POST /client/claims  action=attachments, id → ajouter des pièces à un de mes sinistres
 *
 * Les saisies sont vérifiées ici (Validation) avant l'appel au service ; ClaimService vérifie ensuite
 * ce qui demande la base (contrat du client et actif, date dans la période, nombre de pièces déjà jointes).
 *
 * Limites multipart volontairement larges (30 Mo) : la vraie règle (5 Mo par fichier, 5 fichiers)
 * est vérifiée par Validation.attachments pour afficher un message clair. Au-delà de 30 Mo, Tomcat abandonne la
 * lecture de la requête et le jeton CSRF n'est pas lu : 403. app.js bloque ce cas avant l'envoi.
 */
@WebServlet("/client/claims")
@MultipartConfig(fileSizeThreshold = 1024 * 1024, maxFileSize = 30L * 1024 * 1024, maxRequestSize = 30L * 1024 * 1024)
public class ClientClaimsServlet extends BaseServlet {

    private static final String FILES_FIELD = "attachments";

    private final ClaimService claimService = new ClaimService();
    private final ContractService contractService = new ContractService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        SessionUser user = user(req);
        if (req.getParameter("attachment") != null) {
            ClaimAttachment attachment = claimService.attachment(user, idParam(req, "attachment"));
            sendAttachment(resp, attachment, claimService.fileOf(attachment));
        } else if (req.getParameter("new") != null) {
            showForm(req, resp, user);
        } else if (req.getParameter("id") != null) {
            showDetail(req, resp, user, idParam(req, "id"));
        } else {
            req.setAttribute("claims", claimService.listFor(user));
            render(req, resp, Views.CLIENT_CLAIMS);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        SessionUser user = user(req);
        if ("attachments".equals(RequestParams.get(req, "action"))) {
            addAttachments(req, resp, user);
            return;
        }
        DeclareClaimRequest form = new DeclareClaimRequest(
                RequestParams.get(req, "contractId"),
                RequestParams.get(req, "claimDate"),
                RequestParams.get(req, "type"),
                RequestParams.get(req, "description"),
                RequestParams.get(req, "estimatedAmount"));
        List<UploadedFile> files = files(req);

        // 1. Vérification des saisies par la servlet
        Map<String, String> errors = new LinkedHashMap<>();
        Validation.requiredId(form.contractId(), "contractId", "Choisissez un contrat.", errors);
        Validation.claimDate(form.claimDate(), errors);
        Validation.claimType(form.type(), errors);
        Validation.claimDescription(form.description(), errors);
        Validation.amount(form.estimatedAmount(), "estimatedAmount", "Le montant estimé",
                Validation.CLAIM_AMOUNT_MAX, true, errors);
        Validation.attachments(files, 0, errors);

        // 2. Le service vérifie le contrat (au client, actif) et que le sinistre est pendant le contrat
        if (errors.isEmpty()) {
            try {
                Claim claim = claimService.declare(user, form, files);
                UserSession.flash(req, "Sinistre " + claim.getNumber() + " déclaré.", "success");
                redirect(req, resp, "/client/claims?id=" + claim.getId());
                return;
            } catch (ValidationException e) {
                errors.putAll(e.getErrors());
            }
        }
        req.setAttribute(FormView.ERRORS, errors);
        req.setAttribute("form", form);
        resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        showForm(req, resp, user);
    }

    private void addAttachments(HttpServletRequest req, HttpServletResponse resp, SessionUser user)
            throws ServletException, IOException {
        Long id = idParam(req, "id");
        List<UploadedFile> files = files(req);

        // 1. Vérification des fichiers par la servlet (nombre, taille, type réel)
        Map<String, String> errors = new LinkedHashMap<>();
        if (files.isEmpty()) errors.put("attachments", "Choisissez au moins un fichier.");
        else Validation.attachments(files, 0, errors);

        // 2. Le service vérifie le statut du dossier et le total avec les pièces déjà jointes
        if (errors.isEmpty()) {
            try {
                int added = claimService.addAttachments(user, id, files).size();
                UserSession.flash(req, added + (added > 1 ? " pièces ajoutées." : " pièce ajoutée."), "success");
                redirect(req, resp, "/client/claims?id=" + id + "#attachments");
                return;
            } catch (ValidationException e) {
                errors.putAll(e.getErrors());
            }
        }
        req.setAttribute(FormView.ERRORS, errors);
        resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        showDetail(req, resp, user, id);
    }

    /** Fichiers du champ "attachments" ; un champ laissé vide envoie une partie sans nom, ignorée ici. */
    private static List<UploadedFile> files(HttpServletRequest req) throws IOException, ServletException {
        List<UploadedFile> files = new ArrayList<>();
        for (Part part : req.getParts()) {
            String name = part.getSubmittedFileName();
            if (FILES_FIELD.equals(part.getName()) && name != null && !name.isBlank()) {
                files.add(new UploadedFile(name, part.getSize(), part::getInputStream));
            }
        }
        return files;
    }

    private void showDetail(HttpServletRequest req, HttpServletResponse resp, SessionUser user, Long id)
            throws ServletException, IOException {
        req.setAttribute("claim", claimService.get(user, id));
        req.setAttribute("history", claimService.history(user, id));
        req.setAttribute("attachments", claimService.attachments(user, id));
        render(req, resp, Views.CLIENT_CLAIM);
    }

    private void showForm(HttpServletRequest req, HttpServletResponse resp, SessionUser user)
            throws ServletException, IOException {
        req.setAttribute("contracts", contractService.listFor(user).stream()
                .filter(c -> c.getStatus() == ContractStatus.ACTIF).toList());
        req.setAttribute("claimTypes", ClaimType.values());
        render(req, resp, Views.CLIENT_CLAIM_FORM);
    }
}

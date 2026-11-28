package com.assurcy2000.service;

import com.assurcy2000.dao.ClaimAttachmentDao;
import com.assurcy2000.dao.ClaimCommentDao;
import com.assurcy2000.dao.ClaimDao;
import com.assurcy2000.dao.ClaimHistoryDao;
import com.assurcy2000.dao.ContractDao;
import com.assurcy2000.dao.UserDao;
import com.assurcy2000.dto.request.DeclareClaimRequest;
import com.assurcy2000.dto.request.UploadedFile;
import com.assurcy2000.model.Claim;
import com.assurcy2000.model.ClaimAttachment;
import com.assurcy2000.model.ClaimComment;
import com.assurcy2000.model.ClaimHistory;
import com.assurcy2000.model.Contract;
import com.assurcy2000.model.enums.ClaimStatus;
import com.assurcy2000.model.enums.ClaimType;
import com.assurcy2000.model.enums.ContractStatus;
import com.assurcy2000.persistence.HibernateTransactionRunner;
import com.assurcy2000.persistence.TransactionRunner;
import com.assurcy2000.security.Policy;
import com.assurcy2000.dto.SessionUser;
import com.assurcy2000.service.exceptions.NotFoundException;
import com.assurcy2000.service.exceptions.ValidationException;
import com.assurcy2000.util.AttachmentStorage;
import com.assurcy2000.util.Validation;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.Year;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

import static com.assurcy2000.security.Permission.*;

public class ClaimService {

    private static final Map<ClaimStatus, Set<ClaimStatus>> TRANSITIONS = Map.of(
            ClaimStatus.DECLARE, Set.of(ClaimStatus.EN_ANALYSE),
            ClaimStatus.EN_ANALYSE, Set.of(ClaimStatus.ACCEPTE, ClaimStatus.REFUSE),
            ClaimStatus.ACCEPTE, Set.of(),
            ClaimStatus.REFUSE, Set.of(),
            ClaimStatus.INDEMNISE, Set.of());

    private final ClaimDao claimDao;
    private final ClaimHistoryDao historyDao;
    private final ClaimCommentDao commentDao;
    private final ClaimAttachmentDao attachmentDao;
    private final ContractDao contractDao;
    private final UserDao userDao;
    private final AttachmentStorage storage;
    private final TransactionRunner tx;

    public ClaimService() {
        this(new ClaimDao(), new ClaimHistoryDao(), new ClaimCommentDao(), new ClaimAttachmentDao(),
                new ContractDao(), new UserDao(), new AttachmentStorage(), new HibernateTransactionRunner());
    }

    public ClaimService(ClaimDao claimDao, ClaimHistoryDao historyDao, ClaimCommentDao commentDao,
                        ClaimAttachmentDao attachmentDao, ContractDao contractDao, UserDao userDao,
                        AttachmentStorage storage, TransactionRunner tx) {
        this.claimDao = claimDao;
        this.historyDao = historyDao;
        this.commentDao = commentDao;
        this.attachmentDao = attachmentDao;
        this.contractDao = contractDao;
        this.userDao = userDao;
        this.storage = storage;
        this.tx = tx;
    }

    public List<Claim> listFor(SessionUser actor) {
        if (Policy.readsAll(actor, CLAIM_READ_ALL, CLAIM_READ_OWN)) return tx.inTransaction(claimDao::findAll);
        return tx.inTransaction(() -> claimDao.findByClient(actor.id()));
    }

    public Claim get(SessionUser actor, Long id) {
        return tx.inTransaction(() -> claimDao.findById(id)
                .filter(c -> Policy.canRead(actor, CLAIM_READ_ALL, CLAIM_READ_OWN, c.getContract().getClient().getId()))
                .orElseThrow(() -> new NotFoundException("Sinistre introuvable.")));
    }

    /** Changements de statut d'un sinistre, du plus ancien au plus récent (mêmes droits de lecture que get). */
    public List<ClaimHistory> history(SessionUser actor, Long id) {
        return tx.inTransaction(() -> {
            get(actor, id);
            return historyDao.findByClaim(id);
        });
    }

    /**
     * Commentaires internes d'un sinistre, du plus ancien au plus récent.
     * Notes de travail des agents : lisibles par l'agent et l'admin, jamais par le client.
     */
    public List<ClaimComment> comments(SessionUser actor, Long id) {
        Policy.require(actor, CLAIM_READ_ALL);
        return tx.inTransaction(() -> {
            get(actor, id);
            return commentDao.findByClaim(id);
        });
    }

    /** Ajoute un commentaire de l'agent connecté, quel que soit le statut du sinistre. */
    public ClaimComment addComment(SessionUser actor, Long id, String text) {
        Policy.require(actor, CLAIM_UPDATE);
        Map<String, String> errors = new LinkedHashMap<>();
        String content = Validation.comment(text, errors); // déjà vérifié par la servlet : filet de sécurité
        if (!errors.isEmpty()) throw new ValidationException(errors);
        return tx.inTransaction(() -> {
            Claim claim = get(actor, id);
            return commentDao.save(new ClaimComment(content, claim, userDao.getReference(actor.id())));
        });
    }

    // ---------------------------------------------------------------- pièces justificatives

    /** Pièces justificatives d'un sinistre (mêmes droits de lecture que get). */
    public List<ClaimAttachment> attachments(SessionUser actor, Long claimId) {
        return tx.inTransaction(() -> {
            get(actor, claimId);
            return attachmentDao.findByClaim(claimId);
        });
    }

    /** Une pièce justificative, si l'utilisateur peut lire son sinistre ; sinon « introuvable ». */
    public ClaimAttachment attachment(SessionUser actor, Long attachmentId) {
        return tx.inTransaction(() -> {
            ClaimAttachment attachment = attachmentDao.findById(attachmentId)
                    .orElseThrow(() -> new NotFoundException("Pièce justificative introuvable."));
            get(actor, attachment.getClaim().getId());
            return attachment;
        });
    }

    /** Emplacement du fichier d'une pièce justificative sur le disque. */
    public Path fileOf(ClaimAttachment attachment) {
        return storage.resolve(attachment.getPath());
    }

    /**
     * Le client ajoute des pièces à l'un de ses sinistres, tant que le dossier n'est pas tranché
     * (DECLARE ou EN_ANALYSE), dans la limite de MAX_FILES pièces par sinistre.
     */
    public List<ClaimAttachment> addAttachments(SessionUser actor, Long claimId, List<UploadedFile> files) {
        Policy.require(actor, CLAIM_CREATE_OWN);
        if (files.isEmpty()) {
            throw new ValidationException(Map.of("attachments", "Choisissez au moins un fichier."));
        }
        List<String> stored = new ArrayList<>();
        try {
            return tx.inTransaction(() -> {
                Claim claim = get(actor, claimId);
                if (claim.getStatus() != ClaimStatus.DECLARE && claim.getStatus() != ClaimStatus.EN_ANALYSE) {
                    throw new ValidationException(Map.of("attachments",
                            "Le dossier est traité : il n'est plus possible d'ajouter des pièces."));
                }
                Map<String, String> errors = new LinkedHashMap<>();
                List<AttachmentStorage.Kind> kinds = checkFiles(files, attachmentDao.countByClaim(claimId), errors);
                if (!errors.isEmpty()) throw new ValidationException(errors);
                return saveAttachments(claim, files, kinds, stored);
            });
        } catch (RuntimeException e) {
            stored.forEach(storage::deleteQuietly);   // transaction annulée : pas de fichier orphelin
            throw e;
        }
    }

    /**
     * Vérifie le nombre (avec les pièces déjà jointes), la taille et le type réel des fichiers ;
     * ajoute l'erreur éventuelle sous la clé "attachments" et renvoie le type de chaque fichier.
     * Mêmes règles que la servlet (Validation.attachments) : ici avec le nombre de pièces déjà en base.
     */
    private List<AttachmentStorage.Kind> checkFiles(List<UploadedFile> files, long alreadyAttached,
                                                    Map<String, String> errors) {
        return Validation.attachments(files, alreadyAttached, errors);
    }

    /** Copie les fichiers sur le disque puis les enregistre en base ; stored reçoit les chemins écrits. */
    private List<ClaimAttachment> saveAttachments(Claim claim, List<UploadedFile> files,
                                                  List<AttachmentStorage.Kind> kinds, List<String> stored) {
        List<ClaimAttachment> saved = new ArrayList<>();
        for (int i = 0; i < files.size(); i++) {
            UploadedFile file = files.get(i);
            AttachmentStorage.Kind kind = kinds.get(i);
            String path = storage.store(claim.getId(), kind, file);
            stored.add(path);
            saved.add(attachmentDao.save(new ClaimAttachment(
                    cleanFileName(file.name()), path, kind.mimeType(), file.size(), claim)));
        }
        return saved;
    }

    /** Nom affiché : sans chemin, sans caractères de contrôle, 255 max (voir Validation.fileName). */
    static String cleanFileName(String raw) {
        return Validation.fileName(raw);
    }

    // ---------------------------------------------------------------- déclaration

    public Claim declare(SessionUser actor, DeclareClaimRequest req) {
        return declare(actor, req, List.of());
    }

    /**
     * Vérification du contrat et des fichiers + numéro + insertion du sinistre, de l'historique
     * et des pièces : une seule transaction. Si elle échoue, les fichiers déjà copiés sont supprimés.
     */
    public Claim declare(SessionUser actor, DeclareClaimRequest req, List<UploadedFile> files) {
        Policy.require(actor, CLAIM_CREATE_OWN);
        List<String> stored = new ArrayList<>();
        try {
            return tx.inTransaction(() -> doDeclare(actor, req, files, stored));
        } catch (RuntimeException e) {
            stored.forEach(storage::deleteQuietly);
            throw e;
        }
    }

    private Claim doDeclare(SessionUser actor, DeclareClaimRequest req, List<UploadedFile> files, List<String> stored) {
        Map<String, String> errors = new LinkedHashMap<>();

        // Format des champs : mêmes règles que la servlet (filet de sécurité)
        Long contractId = Validation.requiredId(req.contractId(), "contractId", "Choisissez un contrat.", errors);
        LocalDate date = Validation.claimDate(req.claimDate(), errors);
        ClaimType type = Validation.claimType(req.type(), errors);
        String description = Validation.claimDescription(req.description(), errors);
        BigDecimal amount = Validation.amount(req.estimatedAmount(), "estimatedAmount", "Le montant estimé",
                Validation.CLAIM_AMOUNT_MAX, true, errors);

        // Règles qui demandent la base : contrat du client, actif, et sinistre pendant le contrat
        Contract contract = null;
        if (contractId != null) {
            contract = contractDao.findById(contractId).orElse(null);
            if (contract == null || !contract.getClient().getId().equals(actor.id())) {
                errors.put("contractId", "Contrat introuvable.");
                contract = null;
            } else if (contract.getStatus() != ContractStatus.ACTIF) {
                errors.put("contractId", "Ce contrat n'est pas actif.");
            }
        }
        if (contract != null && date != null && !errors.containsKey("claimDate") && date.isBefore(contract.getStartDate())) {
            errors.put("claimDate", "Le sinistre est antérieur au début du contrat.");
        }

        List<AttachmentStorage.Kind> kinds = checkFiles(files, 0, errors);

        if (!errors.isEmpty()) throw new ValidationException(errors);

        Claim claim = claimDao.save(new Claim(nextNumber(), date, description, type, amount, contract));
        record(claim, null, ClaimStatus.DECLARE, actor);
        saveAttachments(claim, files, kinds, stored);
        return claim;
    }

    public Claim changeStatus(SessionUser actor, Long id, ClaimStatus next) {
        Policy.require(actor, CLAIM_UPDATE);
        return tx.inTransaction(() -> {
            Claim claim = get(actor, id);
            if (!TRANSITIONS.get(claim.getStatus()).contains(next)) {
                throw new ValidationException(Map.of("status",
                        "Passage de " + claim.getStatus() + " à " + next + " impossible."));
            }
            ClaimStatus previous = claim.getStatus();
            claim.setStatus(next);
            claim.setAgent(userDao.getReference(actor.id()));
            Claim updated = claimDao.update(claim);
            record(updated, previous, next, actor);
            return updated;
        });
    }

    public Claim indemnify(SessionUser actor, Long id, String amountText) {
        Policy.require(actor, CLAIM_UPDATE);
        Map<String, String> errors = new LinkedHashMap<>();
        BigDecimal amount = Validation.amount(amountText, "amount", "Le montant", Validation.CLAIM_AMOUNT_MAX, true, errors);
        if (!errors.isEmpty()) throw new ValidationException(errors);
        return tx.inTransaction(() -> {
            Claim claim = get(actor, id);
            if (claim.getStatus() != ClaimStatus.ACCEPTE) {
                throw new ValidationException(Map.of("status", "Seul un sinistre accepté peut être indemnisé."));
            }
            claim.setIndemnifiedAmount(amount);
            claim.setIndemnificationDate(LocalDate.now());
            claim.setStatus(ClaimStatus.INDEMNISE);
            claim.setAgent(userDao.getReference(actor.id()));
            Claim updated = claimDao.update(claim);
            record(updated, ClaimStatus.ACCEPTE, ClaimStatus.INDEMNISE, actor);
            return updated;
        });
    }

    /** Trace un changement de statut dans historique_sinistre, dans la transaction en cours. */
    private void record(Claim claim, ClaimStatus from, ClaimStatus to, SessionUser actor) {
        historyDao.save(new ClaimHistory(claim, from, to, userDao.getReference(actor.id())));
    }

    private String nextNumber() {
        String number;
        do {
            number = "SIN-" + Year.now() + "-" + String.format("%06d", ThreadLocalRandom.current().nextInt(1_000_000));
        } while (claimDao.existsByNumber(number));
        return number;
    }
}

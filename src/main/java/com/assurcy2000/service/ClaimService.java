package com.assurcy2000.service;

import com.assurcy2000.dao.ClaimDao;
import com.assurcy2000.dao.ContractDao;
import com.assurcy2000.dao.UserDao;
import com.assurcy2000.dto.DeclareClaimRequest;
import com.assurcy2000.model.Claim;
import com.assurcy2000.model.Contract;
import com.assurcy2000.model.enums.ClaimStatus;
import com.assurcy2000.model.enums.ClaimType;
import com.assurcy2000.model.enums.ContractStatus;
import com.assurcy2000.security.Policy;
import com.assurcy2000.security.SessionUser;
import com.assurcy2000.service.exceptions.NotFoundException;
import com.assurcy2000.service.exceptions.ValidationException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Year;
import java.time.format.DateTimeParseException;
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

    private static final BigDecimal MAX_AMOUNT = new BigDecimal("10000000");

    private final ClaimDao claimDao;
    private final ContractDao contractDao;
    private final UserDao userDao;

    public ClaimService() { this(new ClaimDao(), new ContractDao(), new UserDao()); }

    public ClaimService(ClaimDao claimDao, ContractDao contractDao, UserDao userDao) {
        this.claimDao = claimDao;
        this.contractDao = contractDao;
        this.userDao = userDao;
    }

    public List<Claim> listFor(SessionUser actor) {
        if (Policy.can(actor, CLAIM_READ_ALL)) return claimDao.findAll();
        Policy.require(actor, CLAIM_READ_OWN);
        return claimDao.findByClient(actor.id());
    }

    public Claim get(SessionUser actor, Long id) {
        Claim claim = claimDao.findById(id).orElseThrow(() -> new NotFoundException("Sinistre introuvable."));
        if (Policy.can(actor, CLAIM_READ_ALL)) return claim;
        if (Policy.can(actor, CLAIM_READ_OWN) && claim.getContract().getClient().getId().equals(actor.id())) return claim;
        throw new NotFoundException("Sinistre introuvable.");
    }

    public Claim declare(SessionUser actor, DeclareClaimRequest req) {
        Policy.require(actor, CLAIM_CREATE_OWN);
        Map<String, String> errors = new LinkedHashMap<>();

        Contract contract = null;
        try {
            Long contractId = Long.valueOf(trim(req.contractId()));
            contract = contractDao.findById(contractId).orElse(null);
            if (contract == null || !contract.getClient().getId().equals(actor.id())) {
                errors.put("contractId", "Contrat introuvable.");
                contract = null;
            } else if (contract.getStatus() != ContractStatus.ACTIF) {
                errors.put("contractId", "Ce contrat n'est pas actif.");
            }
        } catch (NumberFormatException e) {
            errors.put("contractId", "Choisissez un contrat.");
        }

        LocalDate date = null;
        try {
            date = LocalDate.parse(trim(req.claimDate()));
            if (date.isAfter(LocalDate.now())) errors.put("claimDate", "La date du sinistre ne peut pas être dans le futur.");
            else if (contract != null && date.isBefore(contract.getStartDate()))
                errors.put("claimDate", "Le sinistre est antérieur au début du contrat.");
        } catch (DateTimeParseException e) {
            errors.put("claimDate", "La date n'est pas valide.");
        }

        ClaimType type = null;
        try {
            type = ClaimType.valueOf(trim(req.type()));
        } catch (IllegalArgumentException e) {
            errors.put("type", "Type de sinistre invalide.");
        }

        String description = trim(req.description());
        if (description.length() < 10) errors.put("description", "Décrivez le sinistre (10 caractères minimum).");
        else if (description.length() > 2000) errors.put("description", "Description trop longue (2000 caractères max).");

        BigDecimal amount = null;
        try {
            amount = new BigDecimal(trim(req.estimatedAmount()).replace(',', '.'));
            if (amount.signum() <= 0 || amount.compareTo(MAX_AMOUNT) > 0) errors.put("estimatedAmount", "Montant invalide.");
        } catch (NumberFormatException e) {
            errors.put("estimatedAmount", "Le montant n'est pas valide.");
        }

        if (!errors.isEmpty()) throw new ValidationException(errors);

        Claim claim = new Claim(nextNumber(), date, description, type, amount, contract);
        return claimDao.save(claim);
    }

    public Claim changeStatus(SessionUser actor, Long id, ClaimStatus next) {
        Policy.require(actor, CLAIM_UPDATE);
        Claim claim = get(actor, id);
        if (!TRANSITIONS.get(claim.getStatus()).contains(next)) {
            throw new ValidationException(Map.of("status",
                    "Passage de " + claim.getStatus() + " à " + next + " impossible."));
        }
        claim.setStatus(next);
        claim.setAgent(userDao.findById(actor.id()).orElseThrow());
        return claimDao.update(claim);
    }

    public Claim indemnify(SessionUser actor, Long id, String amountText) {
        Policy.require(actor, CLAIM_UPDATE);
        Claim claim = get(actor, id);
        if (claim.getStatus() != ClaimStatus.ACCEPTE) {
            throw new ValidationException(Map.of("status", "Seul un sinistre accepté peut être indemnisé."));
        }
        BigDecimal amount;
        try {
            amount = new BigDecimal(trim(amountText).replace(',', '.'));
        } catch (NumberFormatException e) {
            throw new ValidationException(Map.of("amount", "Le montant n'est pas valide."));
        }
        if (amount.signum() <= 0 || amount.compareTo(MAX_AMOUNT) > 0) {
            throw new ValidationException(Map.of("amount", "Montant invalide."));
        }
        claim.setIndemnifiedAmount(amount);
        claim.setIndemnificationDate(LocalDate.now());
        claim.setStatus(ClaimStatus.INDEMNISE);
        claim.setAgent(userDao.findById(actor.id()).orElseThrow());
        return claimDao.update(claim);
    }

    private String nextNumber() {
        String number;
        do {
            number = "SIN-" + Year.now() + "-" + String.format("%06d", ThreadLocalRandom.current().nextInt(1_000_000));
        } while (claimDao.existsByNumber(number));
        return number;
    }

    private static String trim(String s) { return s == null ? "" : s.trim(); }

}

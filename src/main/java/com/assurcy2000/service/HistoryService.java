package com.assurcy2000.service;

import com.assurcy2000.dao.AiMessageDao;
import com.assurcy2000.dao.ClaimDao;
import com.assurcy2000.dao.ContractDao;
import com.assurcy2000.dto.ChatLine;
import com.assurcy2000.dto.HistoryEntry;
import com.assurcy2000.dto.SessionUser;
import com.assurcy2000.model.Claim;
import com.assurcy2000.model.Contract;
import com.assurcy2000.model.enums.MessageRole;
import com.assurcy2000.persistence.HibernateTransactionRunner;
import com.assurcy2000.persistence.TransactionRunner;
import com.assurcy2000.security.Policy;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static com.assurcy2000.security.Permission.*;

/**
 * Historique des demandes du client connecté (2.1 du sujet) :
 *  - ses conversations avec l'assistant IA (table message_ia) ;
 *  - ses demandes : déclarations de sinistre et souscriptions / renouvellements de contrat.
 *
 * Tout est filtré sur actor.id() (la session), jamais sur un id reçu du navigateur : un client ne peut pas
 * lire l'historique d'un autre, qui contient ses contrats, ses sinistres et ses questions à l'assistant.
 */
public class HistoryService {

    /** Nombre maximum de messages affichés (les plus récents), dans l'ordre chronologique. */
    public static final int CHAT_LIMIT = 100;

    private final ContractDao contractDao;
    private final ClaimDao claimDao;
    private final AiMessageDao messageDao;
    private final TransactionRunner tx;

    public HistoryService() {
        this(new ContractDao(), new ClaimDao(), new AiMessageDao(), new HibernateTransactionRunner());
    }

    public HistoryService(ContractDao contractDao, ClaimDao claimDao, AiMessageDao messageDao, TransactionRunner tx) {
        this.contractDao = contractDao;
        this.claimDao = claimDao;
        this.messageDao = messageDao;
        this.tx = tx;
    }

    /** Conversations avec l'assistant : les {@link #CHAT_LIMIT} derniers messages, du plus ancien au plus récent. */
    public List<ChatLine> conversation(SessionUser actor) {
        Policy.require(actor, AI_ASSISTANT);
        return tx.inTransaction(() -> messageDao.findRecentByClient(actor.id(), CHAT_LIMIT).stream()
                .map(m -> new ChatLine(m.getRole() == MessageRole.USER, m.getContent(), m.getSentAt()))
                .toList());
    }

    /** Déclarations de sinistre et contrats du client, de la plus récente à la plus ancienne. */
    public List<HistoryEntry> requests(SessionUser actor) {
        Policy.require(actor, CLAIM_READ_OWN);
        Policy.require(actor, CONTRACT_READ_OWN);
        return tx.inTransaction(() -> {
            List<HistoryEntry> entries = new ArrayList<>();
            for (Claim c : claimDao.findByClient(actor.id())) {
                entries.add(new HistoryEntry(day(c.getDeclaredAt(), c.getClaimDate()), "Déclaration de sinistre",
                        c.getNumber(), "/client/claims?id=" + c.getId(), c.getStatus().name()));
            }
            for (Contract c : contractDao.findByClient(actor.id())) {
                String type = c.getPreviousContract() != null ? "Renouvellement de contrat" : "Souscription de contrat";
                entries.add(new HistoryEntry(day(c.getCreatedAt(), c.getStartDate()), type,
                        c.getNumber(), "/client/contracts?id=" + c.getId(), c.getStatus().name()));
            }
            entries.sort(Comparator.comparing(HistoryEntry::date, Comparator.reverseOrder())
                    .thenComparing(HistoryEntry::reference, Comparator.reverseOrder()));
            return entries;
        });
    }

    /** Date de la demande : la date d'enregistrement en base, sinon (champ vide) la date métier. */
    private static LocalDate day(LocalDateTime recordedAt, LocalDate fallback) {
        return recordedAt != null ? recordedAt.toLocalDate() : fallback;
    }
}

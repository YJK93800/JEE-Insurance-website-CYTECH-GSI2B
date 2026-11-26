package com.assurcy2000.service;

import com.assurcy2000.dao.PaymentDao;
import com.assurcy2000.dao.UserDao;
import com.assurcy2000.dto.PaymentFilter;
import com.assurcy2000.dto.PaymentSummary;
import com.assurcy2000.dto.SessionUser;
import com.assurcy2000.model.Contract;
import com.assurcy2000.model.Payment;
import com.assurcy2000.model.User;
import com.assurcy2000.model.enums.PaymentMethod;
import com.assurcy2000.model.enums.PaymentStatus;
import com.assurcy2000.persistence.HibernateTransactionRunner;
import com.assurcy2000.persistence.TransactionRunner;
import com.assurcy2000.security.Policy;
import com.assurcy2000.service.exceptions.NotFoundException;
import com.assurcy2000.service.exceptions.ValidationException;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static com.assurcy2000.security.Permission.*;

/**
 * Paiements (section 2.6) : consultation (client : les siens ; agent et admin : tous)
 * et enregistrement d'un paiement par un agent. Les échéances sont créées par PaymentSchedule
 * quand un contrat est créé, modifié, renouvelé ou résilié (voir ContractService).
 */
public class PaymentService {

    /** L'agent peut encaisser une échéance jusqu'à un mois à l'avance (liste du formulaire). */
    static final int PAYABLE_AHEAD_MONTHS = 1;

    private final PaymentDao paymentDao;
    private final ContractService contractService;
    private final UserDao userDao;
    private final TransactionRunner tx;
    private final Clock clock;

    public PaymentService() {
        this(new PaymentDao(), new ContractService(), new UserDao(), new HibernateTransactionRunner(),
                Clock.systemDefaultZone());
    }

    /** clock : « aujourd'hui », fixé dans les tests. */
    public PaymentService(PaymentDao paymentDao, ContractService contractService, UserDao userDao,
                          TransactionRunner tx, Clock clock) {
        this.paymentDao = paymentDao;
        this.contractService = contractService;
        this.userDao = userDao;
        this.tx = tx;
        this.clock = clock;
    }

    public LocalDate today() {
        return LocalDate.now(clock);
    }

    /**
     * Paiements visibles par l'utilisateur, avant le filtre d'onglet :
     * ceux d'un contrat si contractId est donné (404 si le contrat n'est pas accessible),
     * sinon tous (agent, admin) ou les siens (client).
     */
    public List<Payment> visible(SessionUser actor, Long contractId) {
        if (contractId != null) {
            return tx.inTransaction(() -> {
                contractService.get(actor, contractId); // vérifie l'accès au contrat, dans la même transaction
                return paymentDao.findByContract(contractId);
            });
        }
        if (Policy.readsAll(actor, PAYMENT_READ_ALL, PAYMENT_READ_OWN)) return tx.inTransaction(paymentDao::findAll);
        return tx.inTransaction(() -> paymentDao.findByClient(actor.id()));
    }

    /**
     * Applique l'onglet et la recherche (numéro de contrat, nom, prénom ou email du client)
     * à une liste obtenue par visible(), puis la trie.
     */
    public List<Payment> filter(List<Payment> payments, PaymentFilter filter, String query) {
        LocalDate today = today();
        String q = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        return payments.stream()
                .filter(p -> filter.accepts(p, today))
                .filter(p -> q.isEmpty() || matches(p, q))
                .sorted(filter.order())
                .toList();
    }

    public PaymentSummary summary(List<Payment> payments) {
        return PaymentSummary.of(payments, today());
    }

    private static boolean matches(Payment p, String q) {
        Contract c = p.getContract();
        User client = c.getClient();
        return contains(c.getNumber(), q)
                || contains(client.getLastName(), q)
                || contains(client.getFirstName(), q)
                || contains(client.getFirstName() + " " + client.getLastName(), q)
                || contains(client.getEmail(), q);
    }

    private static boolean contains(String value, String q) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(q);
    }

    /** Échéances que l'agent peut encaisser depuis le formulaire : non payées, dues d'ici un mois. */
    public List<Payment> payable(SessionUser actor) {
        Policy.require(actor, PAYMENT_WRITE);
        return tx.inTransaction(() -> paymentDao.findUnpaidDueUntil(today().plusMonths(PAYABLE_AHEAD_MONTHS)));
    }

    /**
     * Enregistre le paiement d'une échéance : statut EFFECTUE, date, mode et agent.
     * Le format des saisies a été vérifié par la servlet ; ici : l'échéance existe, n'est ni payée
     * ni annulée, et la date de paiement n'est pas dans le futur.
     */
    public Payment markPaid(SessionUser actor, Long paymentId, PaymentMethod method, LocalDate paymentDate) {
        Policy.require(actor, PAYMENT_WRITE);
        if (method == null) throw new ValidationException(Map.of("paymentMethod", "Choisissez le mode de paiement."));
        if (paymentDate == null) throw new ValidationException(Map.of("paymentDate", "La date de paiement est obligatoire."));
        if (paymentDate.isAfter(today())) {
            throw new ValidationException(Map.of("paymentDate", "La date de paiement ne peut pas être dans le futur."));
        }
        return tx.inTransaction(() -> {
            Payment payment = paymentDao.findById(paymentId)
                    .orElseThrow(() -> new ValidationException(Map.of("paymentId", "Échéance introuvable.")));
            if (payment.getStatus() == PaymentStatus.EFFECTUE) {
                throw new ValidationException(Map.of("paymentId", "Cette échéance est déjà payée."));
            }
            if (payment.getStatus() == PaymentStatus.ANNULE) {
                throw new ValidationException(Map.of("paymentId", "Cette échéance est annulée (contrat résilié)."));
            }
            payment.setStatus(PaymentStatus.EFFECTUE);
            payment.setPaymentDate(paymentDate);
            payment.setMethod(method);
            payment.setRecordedBy(userDao.getReference(actor.id()));
            return paymentDao.update(payment);
        });
    }

    /** Une échéance précise, pour l'agent (pré-sélection dans le formulaire). */
    public Payment get(SessionUser actor, Long paymentId) {
        Policy.require(actor, PAYMENT_READ_ALL);
        return tx.inTransaction(() -> paymentDao.findById(paymentId)
                .orElseThrow(() -> new NotFoundException("Paiement introuvable.")));
    }

    /**
     * Tâche système (ContractExpirationListener) : les échéances dépassées et non payées des contrats
     * en cours passent EN_RETARD. Pas d'utilisateur connecté, donc pas de vérification de droits.
     * @return nombre d'échéances passées en retard
     */
    public int markOverdue() {
        return tx.inTransaction(() -> paymentDao.markOverdue(today()));
    }
}

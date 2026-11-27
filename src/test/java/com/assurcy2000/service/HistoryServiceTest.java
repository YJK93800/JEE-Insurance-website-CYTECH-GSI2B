package com.assurcy2000.service;

import com.assurcy2000.dao.AiMessageDao;
import com.assurcy2000.dao.ClaimDao;
import com.assurcy2000.dao.ContractDao;
import com.assurcy2000.dto.ChatLine;
import com.assurcy2000.dto.HistoryEntry;
import com.assurcy2000.dto.SessionUser;
import com.assurcy2000.model.AiMessage;
import com.assurcy2000.model.Claim;
import com.assurcy2000.model.Contract;
import com.assurcy2000.model.InsuranceType;
import com.assurcy2000.model.Role;
import com.assurcy2000.model.User;
import com.assurcy2000.model.enums.ClaimStatus;
import com.assurcy2000.model.enums.ClaimType;
import com.assurcy2000.model.enums.ContractStatus;
import com.assurcy2000.model.enums.MessageRole;
import com.assurcy2000.persistence.TransactionRunner;
import com.assurcy2000.service.exceptions.ForbiddenException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;

/** Un client ne voit que SON historique : conversations avec l'assistant, sinistres et contrats. */
class HistoryServiceTest {

    static final TransactionRunner DIRECT = new TransactionRunner() {
        @Override public <R> R inTransaction(Supplier<R> work) { return work.get(); }
    };

    private User aliceUser, bobUser;
    private SessionUser alice, bob, agent, admin;
    private final List<AiMessage> messages = new ArrayList<>();
    private final List<Claim> claims = new ArrayList<>();
    private final List<Contract> contracts = new ArrayList<>();
    private int lastLimit;
    private HistoryService service;

    @BeforeEach
    void setUp() throws Exception {
        aliceUser = withId(new User("Alice", "Martin", "alice@x.fr", "h", new Role(Role.CLIENT)), 1L);
        bobUser = withId(new User("Bob", "Durand", "bob@x.fr", "h", new Role(Role.CLIENT)), 2L);
        alice = new SessionUser(1L, "Alice", "Martin", "alice@x.fr", Role.CLIENT);
        bob = new SessionUser(2L, "Bob", "Durand", "bob@x.fr", Role.CLIENT);
        agent = new SessionUser(3L, "Paul", "Agent", "agent@x.fr", Role.AGENT);
        admin = new SessionUser(4L, "Super", "Admin", "admin@x.fr", Role.ADMIN);

        InsuranceType auto = withId(new InsuranceType("Assurance automobile", "", new BigDecimal("400.00")), 5L);
        LocalDate start = LocalDate.now().minusMonths(6);
        Contract aliceContract = withId(new Contract("CTR-ALICE", start, start.plusYears(1).minusDays(1),
                new BigDecimal("400.00"), aliceUser, auto), 100L);
        aliceContract.setStatus(ContractStatus.ACTIF);
        Contract bobContract = withId(new Contract("CTR-BOB", start.minusMonths(1), start.plusYears(1),
                new BigDecimal("300.00"), bobUser, auto), 200L);
        bobContract.setStatus(ContractStatus.ACTIF);
        contracts.addAll(List.of(aliceContract, bobContract));

        claims.add(withId(new Claim("SIN-ALICE-OLD", LocalDate.now().minusDays(20), "Vitre cassée",
                ClaimType.BRIS, new BigDecimal("300.00"), aliceContract), 50L));
        claims.add(withId(new Claim("SIN-ALICE-NEW", LocalDate.now().minusDays(2), "Vol de rétroviseur",
                ClaimType.VOL, new BigDecimal("150.00"), aliceContract), 51L));
        claims.add(withId(new Claim("SIN-BOB", LocalDate.now().minusDays(5), "Accrochage",
                ClaimType.ACCIDENT, new BigDecimal("900.00"), bobContract), 52L));

        messages.add(new AiMessage(aliceUser, MessageRole.USER, "Quels sont mes contrats actifs ?"));
        messages.add(new AiMessage(aliceUser, MessageRole.ASSISTANT, "Vous avez un contrat automobile."));
        messages.add(new AiMessage(bobUser, MessageRole.USER, "Où en est mon sinistre ?"));

        AiMessageDao messageDao = new AiMessageDao() {
            @Override public List<AiMessage> findRecentByClient(Long clientId, int limit) {
                lastLimit = limit;
                return messages.stream().filter(m -> m.getClient().getId().equals(clientId)).toList();
            }
        };
        ClaimDao claimDao = new ClaimDao() {
            @Override public List<Claim> findByClient(Long clientId) {
                return claims.stream().filter(c -> c.getContract().getClient().getId().equals(clientId)).toList();
            }
        };
        ContractDao contractDao = new ContractDao() {
            @Override public List<Contract> findByClient(Long clientId) {
                return contracts.stream().filter(c -> c.getClient().getId().equals(clientId)).toList();
            }
        };
        service = new HistoryService(contractDao, claimDao, messageDao, DIRECT);
    }

    @Test
    void clientSeesOnlyOwnConversation() {
        List<ChatLine> chat = service.conversation(alice);
        assertEquals(2, chat.size());
        assertTrue(chat.get(0).fromClient());
        assertEquals("Quels sont mes contrats actifs ?", chat.get(0).content());
        assertFalse(chat.get(1).fromClient());
        assertTrue(chat.stream().noneMatch(l -> l.content().contains("sinistre ?")),
                "un message de Bob ne doit pas apparaître chez Alice");

        List<ChatLine> bobChat = service.conversation(bob);
        assertEquals(1, bobChat.size());
        assertEquals("Où en est mon sinistre ?", bobChat.get(0).content());
    }

    @Test
    void conversationIsLimitedToTheMostRecentMessages() {
        service.conversation(alice);
        assertEquals(HistoryService.CHAT_LIMIT, lastLimit);
    }

    @Test
    void clientSeesOnlyOwnRequests() {
        List<HistoryEntry> mine = service.requests(alice);
        List<String> refs = mine.stream().map(HistoryEntry::reference).toList();
        assertTrue(refs.containsAll(List.of("SIN-ALICE-OLD", "SIN-ALICE-NEW", "CTR-ALICE")));
        assertFalse(refs.contains("SIN-BOB"));
        assertFalse(refs.contains("CTR-BOB"));
        assertEquals(3, mine.size());

        List<String> bobRefs = service.requests(bob).stream().map(HistoryEntry::reference).toList();
        assertEquals(List.of("SIN-BOB", "CTR-BOB").size(), bobRefs.size());
        assertTrue(bobRefs.stream().noneMatch(r -> r.contains("ALICE")));
    }

    @Test
    void requestsAreSortedNewestFirstWithLinkAndStatus() {
        List<HistoryEntry> mine = service.requests(alice);
        // sinistre du jour -2, sinistre du jour -20, contrat commencé il y a 6 mois
        assertEquals("SIN-ALICE-NEW", mine.get(0).reference());
        assertEquals("SIN-ALICE-OLD", mine.get(1).reference());
        assertEquals("CTR-ALICE", mine.get(2).reference());

        HistoryEntry newest = mine.get(0);
        assertEquals("Déclaration de sinistre", newest.type());
        assertEquals("/client/claims?id=51", newest.link());
        assertEquals(ClaimStatus.DECLARE.name(), newest.status());

        HistoryEntry contract = mine.get(2);
        assertEquals("Souscription de contrat", contract.type());
        assertEquals("/client/contracts?id=100", contract.link());
        assertEquals(ContractStatus.ACTIF.name(), contract.status());
    }

    @Test
    void emptyHistoryGivesEmptyLists() {
        SessionUser newcomer = new SessionUser(9L, "Neo", "Nouveau", "neo@x.fr", Role.CLIENT);
        assertTrue(service.conversation(newcomer).isEmpty());
        assertTrue(service.requests(newcomer).isEmpty());
    }

    @Test
    void agentsAndAdminsCannotUseTheClientHistory() {
        for (SessionUser staff : List.of(agent, admin)) {
            assertThrows(ForbiddenException.class, () -> service.conversation(staff));
            assertThrows(ForbiddenException.class, () -> service.requests(staff));
        }
    }

    @Test
    void nobodyLoggedInGetsNothing() {
        assertThrows(ForbiddenException.class, () -> service.conversation(null));
        assertThrows(ForbiddenException.class, () -> service.requests(null));
    }

    private static <T> T withId(T entity, Long id) throws Exception {
        Field f = entity.getClass().getDeclaredField("id");
        f.setAccessible(true);
        f.set(entity, id);
        return entity;
    }
}

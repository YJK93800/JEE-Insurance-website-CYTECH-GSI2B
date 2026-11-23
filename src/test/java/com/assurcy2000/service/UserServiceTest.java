package com.assurcy2000.service;

import com.assurcy2000.dao.RoleDao;
import com.assurcy2000.dao.UserDao;
import com.assurcy2000.dto.SessionUser;
import com.assurcy2000.model.Role;
import com.assurcy2000.model.User;
import com.assurcy2000.persistence.TransactionRunner;
import com.assurcy2000.service.exceptions.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;

/** Règles d'accès de UserService.getById, sans base de données. */
class UserServiceTest {

    static final TransactionRunner DIRECT = new TransactionRunner() {
        @Override public <R> R inTransaction(Supplier<R> work) { return work.get(); }
    };

    static class FakeUserDao extends UserDao {
        final Map<Long, User> store;
        FakeUserDao(Map<Long, User> store) { this.store = store; }
        @Override public Optional<User> findById(Long id) { return Optional.ofNullable(store.get(id)); }
    }

    private UserService service;
    private User aliceUser, bobUser, agentUser, adminUser;
    private SessionUser alice, agent, admin;

    @BeforeEach
    void setUp() throws Exception {
        aliceUser = user(1L, "alice", Role.CLIENT);
        bobUser = user(2L, "bob", Role.CLIENT);
        agentUser = user(3L, "agent", Role.AGENT);
        adminUser = user(4L, "admin", Role.ADMIN);
        service = new UserService(
                new FakeUserDao(Map.of(1L, aliceUser, 2L, bobUser, 3L, agentUser, 4L, adminUser)),
                new RoleDao(), DIRECT);

        alice = SessionUser.from(aliceUser);
        agent = SessionUser.from(agentUser);
        admin = SessionUser.from(adminUser);
    }

    @Test
    void everyoneCanReadOwnAccount() {
        assertSame(aliceUser, service.getById(alice, 1L));
        assertSame(agentUser, service.getById(agent, 3L));
    }

    @Test
    void clientCannotReadSomeoneElse() {
        assertThrows(NotFoundException.class, () -> service.getById(alice, 2L));
        assertThrows(NotFoundException.class, () -> service.getById(alice, 4L));
    }

    @Test
    void agentCanReadClientsButNotStaff() {
        assertSame(bobUser, service.getById(agent, 2L));
        assertThrows(NotFoundException.class, () -> service.getById(agent, 4L));
    }

    @Test
    void adminCanReadEveryone() {
        assertSame(agentUser, service.getById(admin, 3L));
        assertSame(bobUser, service.getById(admin, 2L));
    }

    @Test
    void unknownUserIsNotFound() {
        assertThrows(NotFoundException.class, () -> service.getById(admin, 999L));
    }

    private static User user(Long id, String name, String role) throws Exception {
        User u = new User(name, "Test", name + "@x.fr", "hash", new Role(role));
        Field f = User.class.getDeclaredField("id");
        f.setAccessible(true);
        f.set(u, id);
        return u;
    }
}

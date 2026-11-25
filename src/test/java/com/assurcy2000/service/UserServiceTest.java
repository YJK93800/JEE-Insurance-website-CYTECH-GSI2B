package com.assurcy2000.service;

import com.assurcy2000.dao.RoleDao;
import com.assurcy2000.dao.UserDao;
import com.assurcy2000.dto.SessionUser;
import com.assurcy2000.dto.request.UpdateProfileRequest;
import com.assurcy2000.model.Role;
import com.assurcy2000.model.User;
import com.assurcy2000.persistence.TransactionRunner;
import com.assurcy2000.security.Passwords;
import com.assurcy2000.service.exceptions.ForbiddenException;
import com.assurcy2000.service.exceptions.NotFoundException;
import com.assurcy2000.service.exceptions.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Règles de UserService, sans base de données : accès avec getById, modification du profil,
 * changement de mot de passe, activation / désactivation et changement de rôle par l'admin.
 */
class UserServiceTest {

    static final TransactionRunner DIRECT = new TransactionRunner() {
        @Override public <R> R inTransaction(Supplier<R> work) { return work.get(); }
    };

    static class FakeUserDao extends UserDao {
        final Map<Long, User> store;
        FakeUserDao(Map<Long, User> store) { this.store = store; }
        @Override public Optional<User> findById(Long id) { return Optional.ofNullable(store.get(id)); }
        @Override public Optional<User> findByEmail(String email) {
            return store.values().stream().filter(u -> u.getEmail().equals(email)).findFirst();
        }
        @Override public User update(User user) { return user; }
    }

    /** Les trois rôles de V3__donnees_initiales.sql ; tout autre nom est inconnu. */
    static class FakeRoleDao extends RoleDao {
        @Override public Optional<Role> findByName(String name) {
            return switch (name) {
                case Role.CLIENT, Role.AGENT, Role.ADMIN -> Optional.of(new Role(name));
                default -> Optional.empty();
            };
        }
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
                new FakeRoleDao(), DIRECT);

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

    @Test
    void userCanChangeOwnEmailAndBirthDate() {
        LocalDate birth = LocalDate.of(1990, 5, 17);
        User updated = service.updateInfos(alice, new UpdateProfileRequest("alice.new@x.fr", birth, null, null));
        assertEquals("alice.new@x.fr", updated.getEmail());
        assertEquals(birth, updated.getBirthDate());
    }

    @Test
    void keepingOwnEmailIsAllowed() {
        assertDoesNotThrow(() -> service.updateInfos(alice,
                new UpdateProfileRequest("alice@x.fr", LocalDate.of(1990, 1, 1), null, null)));
    }

    @Test
    void emailOfAnotherAccountIsRejected() {
        ValidationException e = assertThrows(ValidationException.class, () -> service.updateInfos(alice,
                new UpdateProfileRequest("bob@x.fr", LocalDate.of(1990, 1, 1), null, null)));
        assertTrue(e.getErrors().containsKey("email"));
        assertEquals("alice@x.fr", aliceUser.getEmail()); // rien n'a été modifié
    }

    // ---- Changement de mot de passe ----

    @Test
    void passwordChangesWithCorrectOldPassword() {
        aliceUser.setPassword(Passwords.hash("ancien1234"));
        service.changePassword(alice, "ancien1234", "nouveau5678");
        assertTrue(Passwords.matches("nouveau5678", aliceUser.getPassword()));
        assertFalse(Passwords.matches("ancien1234", aliceUser.getPassword()));
    }

    @Test
    void wrongOldPasswordIsRejectedAndNothingChanges() {
        String oldHash = Passwords.hash("ancien1234");
        aliceUser.setPassword(oldHash);
        ValidationException e = assertThrows(ValidationException.class,
                () -> service.changePassword(alice, "mauvais999", "nouveau5678"));
        assertTrue(e.getErrors().containsKey("oldPassword"));
        assertEquals(oldHash, aliceUser.getPassword());
    }

    @Test
    void anonymousCannotChangePassword() {
        assertThrows(ForbiddenException.class, () -> service.changePassword(null, "a", "b"));
    }

    // ---- Admin : activer / désactiver un compte ----

    @Test
    void adminCanDeactivateThenReactivateAccount() {
        service.setActive(admin, 2L, false);
        assertFalse(bobUser.isActive());
        service.setActive(admin, 2L, true);
        assertTrue(bobUser.isActive());
    }

    @Test
    void onlyAdminCanChangeActiveFlag() {
        assertThrows(ForbiddenException.class, () -> service.setActive(agent, 2L, false));
        assertThrows(ForbiddenException.class, () -> service.setActive(alice, 2L, false));
        assertTrue(bobUser.isActive());
    }

    @Test
    void adminCannotDeactivateOwnAccount() {
        ValidationException e = assertThrows(ValidationException.class, () -> service.setActive(admin, 4L, false));
        assertTrue(e.getErrors().containsKey("user"));
        assertTrue(adminUser.isActive());
    }

    @Test
    void deactivatingUnknownUserIsNotFound() {
        assertThrows(NotFoundException.class, () -> service.setActive(admin, 999L, false));
    }

    // ---- Admin : changer le rôle ----

    @Test
    void adminCanChangeRole() {
        service.changeRole(admin, 2L, "AGENT");
        assertEquals(Role.AGENT, bobUser.getRole().getName());
    }

    @Test
    void roleNameIsTrimmed() {
        service.changeRole(admin, 2L, "  ADMIN ");
        assertEquals(Role.ADMIN, bobUser.getRole().getName());
    }

    @Test
    void unknownOrMissingRoleIsRejected() {
        ValidationException e = assertThrows(ValidationException.class, () -> service.changeRole(admin, 2L, "SUPERADMIN"));
        assertTrue(e.getErrors().containsKey("role"));
        assertThrows(ValidationException.class, () -> service.changeRole(admin, 2L, null));
        assertEquals(Role.CLIENT, bobUser.getRole().getName());
    }

    @Test
    void onlyAdminCanChangeRole() {
        assertThrows(ForbiddenException.class, () -> service.changeRole(agent, 2L, "ADMIN"));
        assertThrows(ForbiddenException.class, () -> service.changeRole(alice, 1L, "ADMIN"));
        assertEquals(Role.CLIENT, aliceUser.getRole().getName());
    }

    // ---- Correction du nom par un agent ou l'admin ----

    @Test
    void agentCanCorrectClientName() {
        User updated = service.updateName(agent, 2L, "Robert", "Dupont");
        assertEquals("Robert", updated.getFirstName());
        assertEquals("Dupont", bobUser.getLastName());
    }

    @Test
    void agentCannotCorrectStaffName() {
        assertThrows(NotFoundException.class, () -> service.updateName(agent, 4L, "X", "Y"));
        assertEquals("admin", adminUser.getFirstName());
    }

    @Test
    void adminCanCorrectAnyoneElse() {
        service.updateName(admin, 3L, "Agent", "Corrigé");
        service.updateName(admin, 2L, "Bob", "Corrigé");
        assertEquals("Corrigé", agentUser.getLastName());
        assertEquals("Corrigé", bobUser.getLastName());
    }

    @Test
    void clientCannotCorrectNames() {
        assertThrows(ForbiddenException.class, () -> service.updateName(alice, 2L, "X", "Y"));
        assertThrows(ForbiddenException.class, () -> service.updateName(null, 2L, "X", "Y"));
        assertEquals("bob", bobUser.getFirstName());
    }

    @Test
    void nobodyCorrectsOwnNameHere() {
        assertThrows(ValidationException.class, () -> service.updateName(agent, 3L, "X", "Y"));
        assertThrows(ValidationException.class, () -> service.updateName(admin, 4L, "X", "Y"));
        assertEquals("agent", agentUser.getFirstName());
    }

    @Test
    void correctingUnknownUserIsNotFound() {
        assertThrows(NotFoundException.class, () -> service.updateName(admin, 999L, "X", "Y"));
    }

    @Test
    void adminCannotChangeOwnRole() {
        assertThrows(ValidationException.class, () -> service.changeRole(admin, 4L, "CLIENT"));
        assertEquals(Role.ADMIN, adminUser.getRole().getName());
    }

    private static User user(Long id, String name, String role) throws Exception {
        User u = new User(name, "Test", name + "@x.fr", "hash", new Role(role));
        Field f = User.class.getDeclaredField("id");
        f.setAccessible(true);
        f.set(u, id);
        return u;
    }
}

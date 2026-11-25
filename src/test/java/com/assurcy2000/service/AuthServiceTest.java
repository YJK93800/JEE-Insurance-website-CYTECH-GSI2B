package com.assurcy2000.service;

import com.assurcy2000.dao.RoleDao;
import com.assurcy2000.dao.UserDao;
import com.assurcy2000.dto.request.RegisterRequest;
import com.assurcy2000.model.Role;
import com.assurcy2000.model.User;
import com.assurcy2000.persistence.TransactionRunner;
import com.assurcy2000.security.Passwords;
import com.assurcy2000.service.exceptions.AuthException;
import com.assurcy2000.service.exceptions.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;

/** Inscription et connexion (AuthService), sans base de données. */
class AuthServiceTest {

    static final TransactionRunner DIRECT = new TransactionRunner() {
        @Override public <R> R inTransaction(Supplier<R> work) { return work.get(); }
    };

    /** Liste en mémoire à la place de la table utilisateur. */
    static class FakeUserDao extends UserDao {
        final List<User> store = new ArrayList<>();
        @Override public Optional<User> findByEmail(String email) {
            return store.stream().filter(u -> u.getEmail().equals(email.trim().toLowerCase())).findFirst();
        }
        @Override public boolean existsByEmail(String email) { return findByEmail(email).isPresent(); }
        @Override public User save(User user) { store.add(user); return user; }
    }

    static class FakeRoleDao extends RoleDao {
        @Override public Optional<Role> findByName(String name) {
            return Role.CLIENT.equals(name) ? Optional.of(new Role(Role.CLIENT)) : Optional.empty();
        }
    }

    private static final String PASSWORD = "Assurance2026!";

    private FakeUserDao users;
    private AuthService service;

    @BeforeEach
    void setUp() {
        users = new FakeUserDao();
        service = new AuthService(users, new FakeRoleDao(), DIRECT);
    }

    // ---- Inscription ----

    @Test
    void registerCreatesActiveClientWithHashedPassword() {
        User u = service.register(request("  Alice.Martin@X.fr "));

        assertEquals(1, users.store.size());
        assertEquals("alice.martin@x.fr", u.getEmail());          // email nettoyé et en minuscules
        assertEquals(Role.CLIENT, u.getRole().getName());          // toujours client à l'inscription
        assertTrue(u.isActive());
        assertNotEquals(PASSWORD, u.getPassword());                // jamais en clair
        assertTrue(Passwords.matches(PASSWORD, u.getPassword()));
        assertEquals(LocalDate.of(1995, 3, 14), u.getBirthDate());
    }

    @Test
    void emptyOptionalFieldsAreStoredAsNull() {
        User u = service.register(request("alice@x.fr"));
        assertNull(u.getPhone());
        assertNull(u.getAddress());
    }

    @Test
    void emailAlreadyTakenIsRejected() {
        service.register(request("alice@x.fr"));
        ValidationException e = assertThrows(ValidationException.class,
                () -> service.register(request("ALICE@x.fr")));   // même email, casse différente
        assertTrue(e.getErrors().containsKey("email"));
        assertEquals(1, users.store.size());
    }

    // ---- Connexion ----

    @Test
    void loginWithRightPasswordReturnsUser() {
        User registered = service.register(request("alice@x.fr"));
        assertSame(registered, service.login("alice@x.fr", PASSWORD));
    }

    @Test
    void loginIgnoresCaseAndSpacesInEmail() {
        User registered = service.register(request("alice@x.fr"));
        assertSame(registered, service.login("  Alice@X.FR ", PASSWORD));
    }

    @Test
    void wrongPasswordIsRefused() {
        service.register(request("alice@x.fr"));
        assertThrows(AuthException.class, () -> service.login("alice@x.fr", "mauvais123"));
    }

    @Test
    void unknownEmailGivesSameMessageAsWrongPassword() {
        service.register(request("alice@x.fr"));
        AuthException unknown = assertThrows(AuthException.class, () -> service.login("bob@x.fr", PASSWORD));
        AuthException wrong = assertThrows(AuthException.class, () -> service.login("alice@x.fr", "mauvais123"));
        assertEquals(wrong.getMessage(), unknown.getMessage()); // ne pas révéler quels emails existent
    }

    @Test
    void emptyEmailOrPasswordIsRefused() {
        assertThrows(AuthException.class, () -> service.login("", PASSWORD));
        assertThrows(AuthException.class, () -> service.login(null, PASSWORD));
        assertThrows(AuthException.class, () -> service.login("alice@x.fr", ""));
        assertThrows(AuthException.class, () -> service.login("alice@x.fr", null));
    }

    @Test
    void deactivatedAccountCannotLogIn() {
        User registered = service.register(request("alice@x.fr"));
        registered.setActive(false);
        AuthException e = assertThrows(AuthException.class, () -> service.login("alice@x.fr", PASSWORD));
        assertTrue(e.getMessage().contains("désactivé"));
    }

    /** Formulaire déjà validé par RegisterServlet (format vérifié par Validation). */
    private static RegisterRequest request(String email) {
        return new RegisterRequest("Alice", "Martin", email, "", "1995-03-14", "  ", PASSWORD, PASSWORD);
    }
}

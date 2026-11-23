package com.assurcy2000.service;

import com.assurcy2000.dao.RoleDao;
import com.assurcy2000.dao.UserDao;
import com.assurcy2000.dto.request.RegisterRequest;
import com.assurcy2000.model.Role;
import com.assurcy2000.model.User;
import com.assurcy2000.persistence.HibernateTransactionRunner;
import com.assurcy2000.persistence.TransactionRunner;
import com.assurcy2000.service.exceptions.AuthException;
import com.assurcy2000.service.exceptions.ValidationException;
import org.hibernate.exception.ConstraintViolationException;
import org.mindrot.jbcrypt.BCrypt;

import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;

public class AuthService {

    private static final String DUMMY_HASH = BCrypt.hashpw("mot-de-passe-factice", BCrypt.gensalt());
    private final UserDao userDao;
    private final RoleDao roleDao;
    private final TransactionRunner tx;

    public AuthService() {
        this(new UserDao(), new RoleDao(), new HibernateTransactionRunner());
    }

    public AuthService(UserDao userDao, RoleDao roleDao, TransactionRunner tx) {
        this.userDao = userDao;
        this.roleDao = roleDao;
        this.tx = tx;
    }

    /**
     * Crée un compte client. Le format des champs est vérifié avant, par RegisterServlet (classe Validation) ;
     * ici on ne vérifie que ce qui demande la base : l'email déjà utilisé.
     */
    public User register(RegisterRequest req) {
        String email = req.email().trim().toLowerCase();
        String hash = BCrypt.hashpw(req.password(), BCrypt.gensalt()); // calcul lent : hors transaction

        try {
            return tx.inTransaction(() -> {
                if (userDao.existsByEmail(email)) {
                    throw new ValidationException(Map.of("email", "Cet email est déjà utilisé."));
                }
                Role clientRole = roleDao.findByName(Role.CLIENT)
                        .orElseThrow(() -> new IllegalStateException("Le rôle CLIENT n'existe pas en base"));

                User user = new User(req.firstName().trim(), req.lastName().trim(), email, hash, clientRole);
                user.setBirthDate(LocalDate.parse(req.birthDate().trim()));
                user.setPhone(blankToNull(req.phone()));
                user.setAddress(blankToNull(req.address()));
                return userDao.save(user);
            });
        } catch (ConstraintViolationException e) {
            // deux inscriptions simultanées avec le même email : la contrainte UNIQUE tranche, la transaction est annulée
            throw new ValidationException(Map.of("email", "Cet email est déjà utilisé."));
        }
    }

    public User login(String email, String password) {
        email = email == null ? "" : email.trim().toLowerCase();
        if (email == null || email.isBlank() || password == null || password.isEmpty()) {
            throw new AuthException("Email ou mot de passe incorrect.");
        }

        String normalizedEmail = email;
        Optional<User> found = tx.inTransaction(() -> userDao.findByEmail(normalizedEmail));
        String hash = found.map(User::getPassword).orElse(DUMMY_HASH);

        boolean passwordOk;
        try {
            passwordOk = BCrypt.checkpw(password, hash);
        } catch (IllegalArgumentException e) {
            passwordOk = false;
        }

        if (found.isEmpty() || !passwordOk) {
            throw new AuthException("Email ou mot de passe incorrect.");
        }

        User user = found.get();
        if (!user.isActive()) {
            throw new AuthException("Ce compte est désactivé. Contactez un administrateur.");
        }
        return user;
    }

    private static String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }
}

package com.assurcy2000.service;

import com.assurcy2000.dao.RoleDao;
import com.assurcy2000.dao.UserDao;
import com.assurcy2000.dto.request.RegisterRequest;
import com.assurcy2000.model.Role;
import com.assurcy2000.model.User;
import com.assurcy2000.persistence.HibernateTransactionRunner;
import com.assurcy2000.persistence.TransactionRunner;
import com.assurcy2000.security.Passwords;
import com.assurcy2000.service.exceptions.AuthException;
import com.assurcy2000.service.exceptions.ValidationException;
import com.assurcy2000.util.Strings;
import org.hibernate.exception.ConstraintViolationException;

import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;

public class AuthService {

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
        String email = req.email().trim().toLowerCase(); // Mettre l'email en minuscule
        String hash = Passwords.hash(req.password()); // calcul lent : hors transaction

        try {
            return tx.inTransaction(() -> {
                if (userDao.existsByEmail(email)) { // Si l'email est déjà utilisé
                    throw new ValidationException(Map.of("email", "Cet email est déjà utilisé."));
                }
                Role clientRole = roleDao.findByName(Role.CLIENT) // Trouver le role client dans le BDD
                        .orElseThrow(() -> new IllegalStateException("Le rôle CLIENT n'existe pas en base"));

                User user = new User(req.firstName().trim(), req.lastName().trim(), email, hash, clientRole); // Créer l'utilisateur
                user.setBirthDate(LocalDate.parse(req.birthDate().trim())); // Mettre la date de naissance rentrée
                user.setPhone(Strings.blankToNull(req.phone())); //
                user.setAddress(Strings.blankToNull(req.address()));
                return userDao.save(user);
            });
        } catch (ConstraintViolationException e) {
            // deux inscriptions simultanées avec le même email : la contrainte UNIQUE tranche, la transaction est annulée
            throw new ValidationException(Map.of("email", "Cet email est déjà utilisé."));
        }
    }

    public User login(String email, String password) {
        email = Strings.trimToEmpty(email).toLowerCase(); // Mettre l'email en minuscule
        if (email.isBlank() || password == null || password.isEmpty()) { // Email ou mot de passe invalide
            throw new AuthException("Email ou mot de passe incorrect.");
        }

        String normalizedEmail = email;
        Optional<User> found = tx.inTransaction(() -> userDao.findByEmail(normalizedEmail)); // Trouver l'utilisateur avec son email

        // Verifier si le mot de passe est bon (compte introuvable : même temps de calcul, résultat false)
        boolean passwordOk = Passwords.matches(password, found.map(User::getPassword).orElse(null));

        // Renvoyer erreur si mot de passe n'est pas bon, ou l'email n'a pas été rentrée
        if (found.isEmpty() || !passwordOk) {
            throw new AuthException("Email ou mot de passe incorrect.");
        }

        // Verifier si l'utilisateur est active
        User user = found.get();
        if (!user.isActive()) {
            throw new AuthException("Ce compte est désactivé. Contactez un administrateur.");
        }
        return user;
    }
}

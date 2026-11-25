package com.assurcy2000.service;

import com.assurcy2000.dao.RoleDao;
import com.assurcy2000.dao.UserDao;
import com.assurcy2000.model.Role;
import com.assurcy2000.model.User;
import com.assurcy2000.persistence.HibernateTransactionRunner;
import com.assurcy2000.persistence.TransactionRunner;
import com.assurcy2000.security.Passwords;
import com.assurcy2000.security.Policy;
import com.assurcy2000.dto.SessionUser;
import com.assurcy2000.dto.request.UpdateProfileRequest;
import com.assurcy2000.service.exceptions.ForbiddenException;
import com.assurcy2000.service.exceptions.NotFoundException;
import com.assurcy2000.service.exceptions.ValidationException;
import org.hibernate.exception.ConstraintViolationException;

import java.util.List;
import java.util.Map;

import static com.assurcy2000.security.Permission.*;

public class UserService {

    private final UserDao userDao;
    private final RoleDao roleDao;
    private final TransactionRunner tx;

    public UserService() { this(new UserDao(), new RoleDao(), new HibernateTransactionRunner()); }

    public UserService(UserDao userDao, RoleDao roleDao, TransactionRunner tx) {
        this.userDao = userDao;
        this.roleDao = roleDao;
        this.tx = tx;
    }

    public List<User> listAll(SessionUser actor) {
        Policy.require(actor, USER_MANAGE);
        return tx.inTransaction(userDao::findAll);
    }

    public List<User> listClients(SessionUser actor) {
        Policy.require(actor, CLIENT_READ);
        return tx.inTransaction(() -> userDao.findByRole(Role.CLIENT));
    }

    /**
     * Son propre compte : toujours. Admin : tout le monde. Agent : les clients seulement.
     * Sinon « introuvable », pour ne pas révéler qu'un id existe.
     */
    public User getById(SessionUser actor, Long id) {
        Policy.requireLoggedIn(actor);
        return tx.inTransaction(() -> {
            User user = findOrThrow(id);
            if (actor.id().equals(id)) return user;
            if (Policy.can(actor, USER_MANAGE)) return user;
            if (Policy.can(actor, CLIENT_READ) && Role.CLIENT.equals(user.getRole().getName())) return user;
            throw new NotFoundException("Utilisateur introuvable.");
        });
    }

    /** L'utilisateur connecté change son propre mot de passe (le nouveau est validé par la servlet). */
    public void changePassword(SessionUser actor, String oldPassword, String newPassword) {
        Policy.requireLoggedIn(actor);
        String newHash = Passwords.hash(newPassword); // calcul lent : hors transaction
        tx.run(() -> {
            User user = findOrThrow(actor.id());
            if (!Passwords.matches(oldPassword, user.getPassword())) {
                throw new ValidationException(Map.of("oldPassword", "Le mot de passe actuel est incorrect."));
            }
            user.setPassword(newHash);
            userDao.update(user);
        });
    }

    /**
     * L'utilisateur connecté modifie ses informations : email, date de naissance, téléphone, adresse.
     * Le format a déjà été vérifié et nettoyé par la servlet (classe Validation) ;
     * ici on ne vérifie que ce qui demande la base : l'email déjà utilisé par un autre compte.
     */
    public User updateInfos(SessionUser actor, UpdateProfileRequest req) {
        Policy.requireLoggedIn(actor);
        try {
            return tx.inTransaction(() -> {
                User user = findOrThrow(actor.id());
                boolean emailTaken = userDao.findByEmail(req.email())
                        .filter(other -> !other.getId().equals(user.getId()))
                        .isPresent();
                if (emailTaken) {
                    throw new ValidationException(Map.of("email", "Cet email est déjà utilisé."));
                }
                user.setEmail(req.email());
                user.setBirthDate(req.birthDate());
                user.setPhone(req.phone());
                user.setAddress(req.address());
                return userDao.update(user);
            });
        } catch (ConstraintViolationException e) {
            // deux comptes qui prennent le même email en même temps : la contrainte UNIQUE tranche
            throw new ValidationException(Map.of("email", "Cet email est déjà utilisé."));
        }
    }

    public User setActive(SessionUser actor, Long userId, boolean active) {
        Policy.require(actor, USER_MANAGE);
        forbidSelf(actor, userId);
        return tx.inTransaction(() -> {
            User user = findOrThrow(userId);
            user.setActive(active);
            return userDao.update(user);
        });
    }

    public User changeRole(SessionUser actor, Long userId, String roleName) {
        Policy.require(actor, USER_MANAGE);
        forbidSelf(actor, userId);
        return tx.inTransaction(() -> {
            User user = findOrThrow(userId);
            Role role = roleDao.findByName(roleName == null ? "" : roleName.trim())
                    .orElseThrow(() -> new ValidationException(Map.of("role", "Rôle inconnu.")));
            user.setRole(role);
            return userDao.update(user);
        });
    }

    /**
     * Correction du prénom et du nom d'un autre utilisateur (le profil ne permet pas de changer les siens).
     * Admin : tout le monde. Agent : les clients seulement ; un autre agent ou un admin donne « introuvable »,
     * comme getById. Client : interdit.
     * Les valeurs ont déjà été vérifiées et nettoyées par la servlet (classe Validation).
     */
    public User updateName(SessionUser actor, Long userId, String firstName, String lastName) {
        if (!Policy.can(actor, USER_MANAGE) && !Policy.can(actor, CLIENT_READ)) {
            throw new ForbiddenException();
        }
        forbidSelf(actor, userId);
        return tx.inTransaction(() -> {
            User user = findOrThrow(userId);
            boolean allowed = Policy.can(actor, USER_MANAGE) || Role.CLIENT.equals(user.getRole().getName());
            if (!allowed) throw new NotFoundException("Utilisateur introuvable.");
            user.setFirstName(firstName);
            user.setLastName(lastName);
            return userDao.update(user);
        });
    }

    private User findOrThrow(Long id) {
        return userDao.findById(id).orElseThrow(() -> new NotFoundException("Utilisateur introuvable."));
    }

    private static void forbidSelf(SessionUser actor, Long targetId) {
        if (actor.id().equals(targetId)) {
            throw new ValidationException(Map.of("user", "Vous ne pouvez pas modifier votre propre compte ici."));
        }
    }
}

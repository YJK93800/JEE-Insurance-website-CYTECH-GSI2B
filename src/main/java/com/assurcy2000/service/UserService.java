package com.assurcy2000.service;

import com.assurcy2000.dao.RoleDao;
import com.assurcy2000.dao.UserDao;
import com.assurcy2000.model.Role;
import com.assurcy2000.model.User;
import com.assurcy2000.persistence.HibernateTransactionRunner;
import com.assurcy2000.persistence.TransactionRunner;
import com.assurcy2000.security.Policy;
import com.assurcy2000.dto.SessionUser;
import com.assurcy2000.service.exceptions.ForbiddenException;
import com.assurcy2000.service.exceptions.NotFoundException;
import com.assurcy2000.service.exceptions.ValidationException;
import org.mindrot.jbcrypt.BCrypt;

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
        if (actor == null) throw new ForbiddenException();
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
        if (actor == null) throw new ForbiddenException();
        tx.run(() -> {
            User user = findOrThrow(actor.id());
            if (oldPassword == null || !checkPassword(oldPassword, user.getPassword())) {
                throw new ValidationException(Map.of("ancien", "Le mot de passe actuel est incorrect."));
            }
            user.setPassword(BCrypt.hashpw(newPassword, BCrypt.gensalt()));
            userDao.update(user);
        });
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

    private User findOrThrow(Long id) {
        return userDao.findById(id).orElseThrow(() -> new NotFoundException("Utilisateur introuvable."));
    }

    private static boolean checkPassword(String plain, String hash) {
        try {
            return BCrypt.checkpw(plain, hash);
        } catch (IllegalArgumentException e) { // hash mal formé
            return false;
        }
    }

    private static void forbidSelf(SessionUser actor, Long targetId) {
        if (actor.id().equals(targetId)) {
            throw new ValidationException(Map.of("user", "Vous ne pouvez pas modifier votre propre compte ici."));
        }
    }
}

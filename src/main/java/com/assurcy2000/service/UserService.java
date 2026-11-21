package com.assurcy2000.service;

import com.assurcy2000.dao.RoleDao;
import com.assurcy2000.dao.UserDao;
import com.assurcy2000.model.Role;
import com.assurcy2000.model.User;
import com.assurcy2000.security.Policy;
import com.assurcy2000.dto.SessionUser;
import com.assurcy2000.service.exceptions.NotFoundException;
import com.assurcy2000.service.exceptions.ValidationException;

import java.util.List;
import java.util.Map;

import static com.assurcy2000.security.Permission.*;

public class UserService {

    private final UserDao userDao;
    private final RoleDao roleDao;

    public UserService() { this(new UserDao(), new RoleDao()); }

    public UserService(UserDao userDao, RoleDao roleDao) {
        this.userDao = userDao;
        this.roleDao = roleDao;
    }

    public List<User> listAll(SessionUser actor) {
        Policy.require(actor, USER_MANAGE);
        return userDao.findAll();
    }

    public List<User> listClients(SessionUser actor) {
        Policy.require(actor, CLIENT_READ);
        return userDao.findByRole(Role.CLIENT);
    }

    public User setActive(SessionUser actor, Long userId, boolean active) {
        Policy.require(actor, USER_MANAGE);
        forbidSelf(actor, userId);
        User user = userDao.findById(userId).orElseThrow(() -> new NotFoundException("Utilisateur introuvable."));
        user.setActive(active);
        return userDao.update(user);
    }

    public User changeRole(SessionUser actor, Long userId, String roleName) {
        Policy.require(actor, USER_MANAGE);
        forbidSelf(actor, userId);
        User user = userDao.findById(userId).orElseThrow(() -> new NotFoundException("Utilisateur introuvable."));
        Role role = roleDao.findByName(roleName == null ? "" : roleName.trim())
                .orElseThrow(() -> new ValidationException(Map.of("role", "Rôle inconnu.")));
        user.setRole(role);
        return userDao.update(user);
    }

    private static void forbidSelf(SessionUser actor, Long targetId) {
        if (actor.id().equals(targetId)) {
            throw new ValidationException(Map.of("user", "Vous ne pouvez pas modifier votre propre compte ici."));
        }
    }
}

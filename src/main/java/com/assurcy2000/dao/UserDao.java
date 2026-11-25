package com.assurcy2000.dao;

import com.assurcy2000.model.Role;
import com.assurcy2000.model.User;

import java.util.List;
import java.util.Optional;

public class UserDao extends BaseDao<User, Long> {

    public UserDao() { super(User.class); }

    @Override
    public Optional<User> findById(Long id) {
        return query(s -> s.createQuery(
                        "select u from User u join fetch u.role where u.id = :id", User.class)
                .setParameter("id", id)
                .uniqueResultOptional());
    }

    @Override
    public List<User> findAll() {
        return query(s -> s.createQuery(
                        "select u from User u join fetch u.role order by u.lastName, u.firstName", User.class)
                .getResultList());
    }

    public Optional<User> findByEmail(String email) {
        return query(s -> s.createQuery(
                        "select u from User u join fetch u.role where u.email = :email", User.class)
                .setParameter("email", email.trim().toLowerCase())
                .uniqueResultOptional());
    }

    public boolean existsByEmail(String email) {
        return query(s -> s.createQuery(
                        "select count(u) from User u where u.email = :email", Long.class)
                .setParameter("email", email.trim().toLowerCase())
                .getSingleResult() > 0);
    }

    public List<User> findByRole(String roleName) {
        return query(s -> s.createQuery(
                        "select u from User u join fetch u.role r where r.name = :role order by u.lastName",
                        User.class)
                .setParameter("role", roleName)
                .getResultList());
    }

    /**
     * Clients dont le prénom, le nom, « prénom nom », l'email ou le téléphone contient le mot-clé
     * (sans tenir compte de la casse). Mot-clé vide : tous les clients.
     */
    public List<User> searchClients(String keyword) {
        String k = keyword == null ? "" : keyword.trim().toLowerCase();
        StringBuilder hql = new StringBuilder("select u from User u join fetch u.role r where r.name = :role");
        if (!k.isEmpty()) {
            hql.append(" and (lower(u.firstName) like :p or lower(u.lastName) like :p or lower(u.email) like :p"
                    + " or lower(concat(u.firstName, ' ', u.lastName)) like :p or u.phone like :p)");
        }
        hql.append(" order by u.lastName, u.firstName");
        return query(s -> {
            var q = s.createQuery(hql.toString(), User.class).setParameter("role", Role.CLIENT);
            if (!k.isEmpty()) q.setParameter("p", "%" + k + "%");
            return q.getResultList();
        });
    }
}
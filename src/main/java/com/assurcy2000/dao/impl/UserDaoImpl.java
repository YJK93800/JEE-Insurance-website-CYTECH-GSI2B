package com.assurcy2000.dao.impl;

import com.assurcy2000.dao.UserDao;
import com.assurcy2000.model.User;

import java.util.List;
import java.util.Optional;

public class UserDaoImpl extends HibernateDao<User, Long> implements UserDao {

    public UserDaoImpl() {
        super(User.class);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return session().createSelectionQuery(
                        "from User u where u.email = :email", User.class)
                .setParameter("email", email)
                .uniqueResultOptional();
    }

    @Override
    public Optional<User> findByPhoneNumber(String phoneNumber) {
        return session().createSelectionQuery(
                        "from User u where u.phoneNumber = :phone", User.class)
                .setParameter("phone", phoneNumber)
                .uniqueResultOptional();
    }

    @Override
    public List<User> findByRole(String roleName) {
        // HQL : on utilise les noms des attributs Java (role.name), pas ceux des colonnes SQL (nom)
        return session().createSelectionQuery(
                        "from User u where u.role.name = :r", User.class)
                .setParameter("r", roleName)
                .getResultList();
    }
}

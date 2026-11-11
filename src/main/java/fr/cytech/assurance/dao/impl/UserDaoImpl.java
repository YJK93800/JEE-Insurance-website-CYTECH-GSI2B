package fr.cytech.assurance.dao.impl;

import fr.cytech.assurance.dao.UserDao;
import fr.cytech.assurance.model.User;
import fr.cytech.assurance.model.enums.Role;

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
    public List<User> findByRole(Role role) {
        // role est un enum : on compare directement, pas de ".nom"
        return session().createSelectionQuery(
                        "from User u where u.role = :r", User.class)
                .setParameter("r", role)
                .getResultList();
    }
}

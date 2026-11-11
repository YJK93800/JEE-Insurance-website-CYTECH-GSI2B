package fr.cytech.assurance.dao;

import fr.cytech.assurance.model.User;
import fr.cytech.assurance.model.enums.Role;

import java.util.List;
import java.util.Optional;

public interface UserDao extends BaseDao<User, Long> {
    Optional<User> findByEmail(String email);
    Optional<User> findByPhoneNumber(String phoneNumber);
    List<User> findByRole(Role role);
}

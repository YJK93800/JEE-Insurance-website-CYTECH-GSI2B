package fr.cytech.assurance.dao;

import fr.cytech.assurance.model.Role;

import java.util.Optional;

public interface RoleDao extends BaseDao<Role, Long> {
    Optional<Role> findByName(String name);
}

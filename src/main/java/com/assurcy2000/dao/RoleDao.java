package com.assurcy2000.dao;

import com.assurcy2000.model.Role;

import java.util.Optional;

public class RoleDao extends BaseDao<Role, Long> {

    public RoleDao() { super(Role.class); }

    public Optional<Role> findByName(String name) {
        return query(s -> s.createQuery("from Role r where r.name = :name", Role.class)
                .setParameter("name", name)
                .uniqueResultOptional());
    }
}
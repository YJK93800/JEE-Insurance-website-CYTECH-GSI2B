package fr.cytech.assurance.dao.impl;

import fr.cytech.assurance.dao.RoleDao;
import fr.cytech.assurance.model.Role;

import java.util.Optional;

public class RoleDaoImpl extends HibernateDao<Role, Long> implements RoleDao {

    public RoleDaoImpl() {
        super(Role.class);
    }

    @Override
    public Optional<Role> findByName(String name) {
        return session().createSelectionQuery(
                        "from Role r where r.name = :name", Role.class) // attribut Java "name", pas la colonne "nom"
                .setParameter("name", name)
                .uniqueResultOptional();
    }
}

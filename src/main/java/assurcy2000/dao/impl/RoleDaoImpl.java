package assurcy2000.dao.impl;

import assurcy2000.dao.RoleDao;
import assurcy2000.model.Role;

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

package assurcy2000.dao;

import assurcy2000.model.Role;

import java.util.Optional;

public interface RoleDao extends BaseDao<Role, Long> {
    Optional<Role> findByName(String name);
}

package com.nnmilestoempty.base.repository.auth;

import com.nnmilestoempty.base.model.dao.auth.Role;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleRepository extends JpaRepository<Role, Long> {
}

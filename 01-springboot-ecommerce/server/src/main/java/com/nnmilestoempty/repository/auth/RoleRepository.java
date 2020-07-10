package com.nnmilestoempty.repository.auth;

import com.nnmilestoempty.model.dao.auth.Role;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleRepository extends JpaRepository<Role, Long> {
}

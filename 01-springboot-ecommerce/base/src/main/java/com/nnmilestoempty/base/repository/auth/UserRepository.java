package com.nnmilestoempty.base.repository.auth;

import com.nnmilestoempty.base.model.dao.auth.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
    User findByUsername(String username);
}

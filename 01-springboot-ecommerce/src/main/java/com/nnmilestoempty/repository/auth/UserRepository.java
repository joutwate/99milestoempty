package com.nnmilestoempty.repository.auth;

import com.nnmilestoempty.model.auth.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
    User findByUsername(String username);
}

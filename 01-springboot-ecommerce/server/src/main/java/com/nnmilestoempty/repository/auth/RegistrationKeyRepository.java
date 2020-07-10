package com.nnmilestoempty.repository.auth;

import com.nnmilestoempty.model.dao.auth.RegistrationToken;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RegistrationKeyRepository extends JpaRepository<RegistrationToken, Long> {
    RegistrationToken findFirstByTokenOrderByCreatedDesc(String val);

    /**
     * Only used for test cases.
     */
    RegistrationToken findFirstByUsername(String username);
}

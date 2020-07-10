package com.nnmilestoempty.security;

import com.nnmilestoempty.model.dao.auth.User;
import com.nnmilestoempty.repository.auth.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.UserDetailsManager;
import org.springframework.stereotype.Component;

@Component
public class CustomUserDetailsManager implements UserDetailsManager {
    private final UserRepository userRepository;

    private final PasswordEncoder passwordEncoder;

    @Autowired
    public CustomUserDetailsManager(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username);
        if (null == user) {
            throw new UsernameNotFoundException("No user present with username: " + username);
        } else {
            return new CustomUserDetails(user);
        }
    }

    @Override
    public void createUser(UserDetails userDetails) {
        User user = ((CustomUserDetails) userDetails).getUser();
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        userRepository.save(user);
    }

    @Override
    public void updateUser(UserDetails userDetails) {
        // Get the user by id.  If the username doesn't match the id then don't do anything
        User user = ((CustomUserDetails) userDetails).getUser();
        userRepository.save(user);
    }

    @Override
    public void deleteUser(String username) {
        CustomUserDetails details;
        try {
            details = (CustomUserDetails) loadUserByUsername(username);
        } catch (UsernameNotFoundException ex) {
            return;
        }

        User user = details.getUser();
        user.setEnabled(false);
        userRepository.save(user);
    }

    @Override
    public void changePassword(String oldPassword, String newPassword) {
        Authentication currentUser = SecurityContextHolder.getContext()
                .getAuthentication();
        CustomUserDetails details = (CustomUserDetails) currentUser.getDetails();
        // Validate old password is correct.
        if (passwordEncoder.matches(oldPassword, details.getPassword())) {
            // The password was valid, go ahead and update this user's password and persist it.
            User user = details.getUser();
            user.setPassword(passwordEncoder.encode(newPassword));
            userRepository.save(user);
        }
    }

    @Override
    public boolean userExists(String s) {
        return userRepository.findByUsername(s) != null;
    }
}

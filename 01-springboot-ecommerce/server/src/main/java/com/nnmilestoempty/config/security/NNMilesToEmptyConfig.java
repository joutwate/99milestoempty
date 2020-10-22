package com.nnmilestoempty.config.security;

import com.google.common.collect.ImmutableList;
import com.nnmilestoempty.base.auth.CustomUserDetails;
import com.nnmilestoempty.base.auth.CustomUserDetailsManager;
import com.nnmilestoempty.base.auth.JwtAuthenticationFilter;
import com.nnmilestoempty.base.model.dao.auth.Role;
import com.nnmilestoempty.base.model.dao.auth.User;
import com.nnmilestoempty.base.repository.auth.UserRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Collections;

/**
 * Spring security configuration that introduces a custom user details service, JWT authentication, password encoder
 * for encryption and a test user that can be used to verify endpoint security is enabled as expected.
 */
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class NNMilesToEmptyConfig extends WebSecurityConfigurerAdapter {

    private final UserRepository userRepository;

    public NNMilesToEmptyConfig(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter() {
        return new JwtAuthenticationFilter();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration corsConf = new CorsConfiguration();
        corsConf.addAllowedOrigin("*");
        corsConf.setAllowedMethods(ImmutableList.of("*"));
        corsConf.setAllowCredentials(true);
        corsConf.setAllowedHeaders(ImmutableList.of("*"));
        UrlBasedCorsConfigurationSource result = new UrlBasedCorsConfigurationSource();
        result.registerCorsConfiguration("/**", corsConf);
        return result;
    }

    @Bean
    protected UserDetailsService userDetailsService() {
        return new CustomUserDetailsManager(userRepository, passwordEncoder());
    }

    @Override
    protected void configure(final AuthenticationManagerBuilder auth) throws Exception {
        CustomUserDetailsManager manager = (CustomUserDetailsManager) userDetailsService();
        auth.userDetailsService(manager).passwordEncoder(passwordEncoder());

        // Create two users that we can use for testing our endpoint security. One that has the role of user and another
        // with the role of admin.
        if (!manager.userExists("admin")) {
            Role role = new Role("ROLE_ADMIN");
            User user = new User("Test", "Admin", "test_admin@99milestoempty.com", "admin", "password", true);
            role.setUser(user);
            user.setRoles(Collections.singleton(role));
            UserDetails details = new CustomUserDetails(user);
            manager.createUser(details);
        }

        if (!manager.userExists("user")) {
            Role role = new Role("ROLE_USER");
            User user = new User("Test", "User", "test_user@99milestoempty.com", "user", "password", true);
            role.setUser(user);
            user.setRoles(Collections.singleton(role));
            UserDetails details = new CustomUserDetails(user);
            manager.createUser(details);
        }
    }

    /**
     * Configure our system to have stateless session management and two test URLs that are accessible based on specific
     * roles. We also insert out JWT authentication filter prior to the standard Spring username and password
     * authentication filter so we can support authentication via JWT.
     */
    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
                .cors()
                .and()
                .csrf().disable()
                .exceptionHandling()
                .and()
                .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                .and()
                .authorizeRequests()
                .antMatchers("/favicon.ico").permitAll()
                .antMatchers("/register/**").permitAll()
                .antMatchers("/login").permitAll()
                .antMatchers("/user").hasAnyRole("USER", "ADMIN")
                .antMatchers("/admin").hasRole("ADMIN")
                .antMatchers("/actuator/**").hasRole("ADMIN")
                .anyRequest().authenticated();

        // Add our custom JWT security filter
        http.addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class);
    }
}

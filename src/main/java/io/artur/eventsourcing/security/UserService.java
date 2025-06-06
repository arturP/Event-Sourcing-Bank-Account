package io.artur.eventsourcing.security;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Collections;

@Service
public class UserService implements UserDetailsService {
    
    private final PasswordEncoder passwordEncoder;
    private final Map<String, UserDetails> users = new HashMap<>();
    
    public UserService(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
        initializeDefaultUsers();
    }
    
    private void initializeDefaultUsers() {
        // Create demo users
        users.put("admin", User.builder()
                .username("admin")
                .password(passwordEncoder.encode("admin123"))
                .roles("ADMIN", "USER")
                .build());
                
        users.put("user", User.builder()
                .username("user")
                .password(passwordEncoder.encode("user123"))
                .roles("USER")
                .build());
                
        users.put("demo", User.builder()
                .username("demo")
                .password(passwordEncoder.encode("demo123"))
                .roles("USER")
                .build());
    }
    
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        UserDetails user = users.get(username);
        if (user == null) {
            throw new UsernameNotFoundException("User not found: " + username);
        }
        return user;
    }
    
    public boolean validateUser(String username, String password) {
        try {
            UserDetails user = loadUserByUsername(username);
            return passwordEncoder.matches(password, user.getPassword());
        } catch (UsernameNotFoundException e) {
            return false;
        }
    }
}
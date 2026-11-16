package com.dept.budget.entity;

import jakarta.persistence.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * The staff member who logs in.
 * Only the BCrypt hash of the password is stored, never the password itself.
 */
@Entity
@Table(name = "staff")
public class Staff {

    private static final BCryptPasswordEncoder ENCODER = new BCryptPasswordEncoder();

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer staffId;

    private String username;

    private String passwordHash;

    protected Staff() {
        // needed by JPA
    }

    public Staff(String username, String rawPassword) {
        this.username = username;
        this.passwordHash = ENCODER.encode(rawPassword);
    }

    /** Checks a typed password against the stored hash. */
    public boolean login(String rawPassword) {
        return rawPassword != null && ENCODER.matches(rawPassword, passwordHash);
    }

    public Integer getStaffId() { return staffId; }
    public String getUsername() { return username; }
}

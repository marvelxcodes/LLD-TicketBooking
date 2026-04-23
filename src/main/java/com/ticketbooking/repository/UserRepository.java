package com.ticketbooking.repository;

import com.ticketbooking.model.User;
import com.ticketbooking.enums.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for User entity operations.
 * Provides CRUD operations and custom query methods.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Finds a user by their username.
     */
    Optional<User> findByUsername(String username);

    /**
     * Finds a user by their email address.
     */
    Optional<User> findByEmail(String email);

    /**
     * Checks if a user exists with the given username.
     */
    boolean existsByUsername(String username);

    /**
     * Checks if a user exists with the given email.
     */
    boolean existsByEmail(String email);

    /**
     * Finds all users with a specific role.
     */
    List<User> findByRole(UserRole role);

    /**
     * Finds all active users.
     */
    List<User> findByIsActiveTrue();
}

package com.example.legalai.repository;

import com.example.legalai.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;

@Repository
public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {
    
    Optional<User> findByUsernameAndActiveTrue(String username);
    
    Optional<User> findByEmailAndActiveTrue(String email);
    
    boolean existsByUsernameAndActiveTrue(String username);
    
    boolean existsByEmailAndActiveTrue(String email);
    
    @Query("SELECT u FROM User u WHERE u.active = true AND u.roles = :role")
    Page<User> findByRoleAndActiveTrue(@Param("role") User.UserRole role, Pageable pageable);
    
    @Query("SELECT u FROM User u WHERE u.active = true AND " +
           "(LOWER(u.username) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(u.firstName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(u.lastName) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<User> findBySearchTermAndActiveTrue(@Param("search") String searchTerm, Pageable pageable);
    
    @Query("SELECT COUNT(u) FROM User u WHERE u.createdAt >= :since")
    long countUsersCreatedSince(@Param("since") LocalDateTime since);
    
    @Query("SELECT u.roles FROM User u WHERE u.id = :userId AND u.active = true")
    Set<User.UserRole> findUserRolesById(@Param("userId") Long userId);
}
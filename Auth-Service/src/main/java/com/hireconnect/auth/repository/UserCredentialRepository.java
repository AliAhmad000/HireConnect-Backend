package com.hireconnect.auth.repository;

import com.hireconnect.auth.entity.UserCredential;
import com.hireconnect.auth.enums.UserRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserCredentialRepository extends JpaRepository<UserCredential, Long> {

    Optional<UserCredential> findByEmail(String email);

    boolean existsByEmail(String email);

    Optional<UserCredential> findByRefreshToken(String refreshToken);

    Optional<UserCredential> findByPasswordResetToken(String passwordResetToken);

    Optional<UserCredential> findByProviderIdAndProvider(String providerId, com.hireconnect.auth.enums.AuthProvider provider);

    long countByRole(UserRole role);

    List<UserCredential> findByRoleAndIsActiveTrue(UserRole role);

    @Query("""
            SELECT u FROM UserCredential u
            WHERE (:role IS NULL OR u.role = :role)
              AND (:active IS NULL OR u.isActive = :active)
              AND (
                :search IS NULL OR :search = '' OR
                LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%')) OR
                LOWER(COALESCE(u.fullName, '')) LIKE LOWER(CONCAT('%', :search, '%'))
              )
            """)
    Page<UserCredential> searchUsers(@Param("role") UserRole role,
                                     @Param("active") Boolean active,
                                     @Param("search") String search,
                                     Pageable pageable);

    @Modifying
    @Query("UPDATE UserCredential u SET u.lastLogin = :loginTime WHERE u.userId = :id")
    void updateLastLogin(@Param("id") Long userId, @Param("loginTime") LocalDateTime loginTime);

    @Modifying
    @Query("UPDATE UserCredential u SET u.refreshToken = :token, u.refreshTokenExpiry = :expiry WHERE u.userId = :id")
    void updateRefreshToken(@Param("id") Long userId,
                            @Param("token") String token,
                            @Param("expiry") LocalDateTime expiry);

    @Modifying
    @Query("UPDATE UserCredential u SET u.isActive = :active WHERE u.userId = :id")
    void updateActiveStatus(@Param("id") Long userId, @Param("active") boolean active);
}

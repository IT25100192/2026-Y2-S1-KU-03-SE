package com.starvoicelanka.user.repository;

import com.starvoicelanka.user.entity.Role;
import com.starvoicelanka.user.entity.User;
import com.starvoicelanka.user.entity.UserStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {
    Optional<User> findByEmailIgnoreCase(String email);
    boolean existsByEmailIgnoreCase(String email);

    long countByRoleAndStatusAndClosedAtIsNull(Role role, UserStatus status);
    long countByRoleAndStatusAndClosedAtIsNullAndIdNot(Role role, UserStatus status, Long id);

    List<User> findByRoleAndStatusAndClosedAtIsNull(Role role, UserStatus status);
    List<User> findByRoleAndStatus(Role role, UserStatus status);
    List<User> findByStatus(UserStatus status);
}

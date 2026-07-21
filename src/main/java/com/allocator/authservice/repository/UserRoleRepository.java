package com.allocator.authservice.repository;

import com.allocator.authservice.model.Brand;
import com.allocator.authservice.model.User;
import com.allocator.authservice.model.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface UserRoleRepository extends JpaRepository<UserRole, UUID> {
    List<UserRole> findByUser(User user);

    List<UserRole> findByUserAndBrand(User user, Brand brand);

    long countDistinctUserByBrand(Brand brand);

    @Query("SELECT DISTINCT ur.user FROM UserRole ur WHERE ur.brand = :brand")
    List<User> findDistinctUsersByBrand(@Param("brand") Brand brand);
}

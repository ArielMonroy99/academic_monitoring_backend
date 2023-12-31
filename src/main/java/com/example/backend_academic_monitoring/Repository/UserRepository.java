package com.example.backend_academic_monitoring.Repository;

import com.example.backend_academic_monitoring.Entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, Integer> {
   UserEntity findByUsername(String username);
   boolean existsByUsername(String username);
}

package com.example.security.repository;

import com.example.security.entity.UserEntity;
import org.apache.catalina.User;
import org.springframework.data.jpa.repository.JpaRepository;

import javax.swing.text.html.Option;
import java.util.Optional;

public interface UserEntityRepository extends JpaRepository<UserEntity, Integer> {
    Optional<UserEntity> findByUsername(String username);
    Optional<UserEntity> findByEmail(String email);
    Boolean existsByUsername(String username);
}

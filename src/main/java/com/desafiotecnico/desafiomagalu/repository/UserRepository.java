package com.desafiotecnico.desafiomagalu.repository;

import com.desafiotecnico.desafiomagalu.model.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserRepository extends JpaRepository<UserEntity, Long> {
    Optional<UserEntity> findByUserId(Long userId);
}

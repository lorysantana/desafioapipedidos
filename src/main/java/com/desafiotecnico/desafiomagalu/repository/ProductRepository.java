package com.desafiotecnico.desafiomagalu.repository;

import com.desafiotecnico.desafiomagalu.model.ProductEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<ProductEntity, Long> {
}

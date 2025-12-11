package com.nhom1.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.nhom1.entity.Inventory;

public interface InventoryRepository extends JpaRepository<Inventory, Long> {

    Optional<Inventory> findByProductId(String productId);
}

package com.nhom1.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.nhom1.entity.Order;

public interface OrderRepository extends JpaRepository<Order, Long> {
}

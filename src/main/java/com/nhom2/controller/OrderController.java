package com.nhom1.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nhom1.entity.Order;
import com.nhom1.service.OrderService;

@RestController
@RequestMapping("/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    // =============== TẠO / LẤY ĐƠN ===============

    @PostMapping
    public ResponseEntity<Map<String, Object>> createOrder(@RequestBody Order orderRequest) {
        Order saved = orderService.createOrder(orderRequest);
        return ResponseEntity.ok(
                Map.of(
                        "message", "Đơn hàng của bạn đã được ghi nhận. Vui lòng kiểm tra email xác nhận.",
                        "orderId", saved.getId()
                )
        );
    }

    @GetMapping
    public List<Order> getAllOrders() {
        return orderService.getAllOrders();
    }

    @GetMapping("/{id}")
    public Order getOrder(@PathVariable Long id) {
        return orderService.getOrder(id);
    }

    // =============== TRA CỨU TRẠNG THÁI ===============

    @GetMapping("/{id}/status")
    public Map<String, Object> getOrderStatus(@PathVariable Long id) {
        Order order = orderService.getOrder(id);
        return Map.of(
                "orderId", order.getId(),
                "emailSent", order.isEmailSent(),
                "stockUpdated", order.isStockUpdated(),
                "logWritten", order.isLogWritten(),
                "cancelled", order.isCancelled()
        );
    }

    // =============== THỐNG KÊ ===============

    @GetMapping("/stats")
    public Map<String, Long> getOrderStats() {
        return orderService.getOrderStats();
    }

    // =============== HUỶ ĐƠN + KHÔI PHỤC KHO ===============

    @PostMapping("/{id}/cancel")
    public ResponseEntity<Map<String, Object>> cancelOrder(@PathVariable Long id) {
        Order cancelled = orderService.cancelOrder(id);

        return ResponseEntity.ok(
                Map.of(
                        "message", "Đã huỷ đơn #" + cancelled.getId(),
                        "orderId", cancelled.getId(),
                        "cancelled", cancelled.isCancelled()
                )
        );
    }
}

package com.nhom1.service;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nhom1.entity.Order;
import com.nhom1.messaging.OrderProducer;
import com.nhom1.repository.OrderRepository;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderProducer orderProducer;
    private final InventoryService inventoryService;   // NEW: dùng để cộng lại kho khi huỷ đơn

    public OrderService(OrderRepository orderRepository,
                        OrderProducer orderProducer,
                        InventoryService inventoryService) {
        this.orderRepository = orderRepository;
        this.orderProducer = orderProducer;
        this.inventoryService = inventoryService;
    }

    // ================== CRUD / BIZ LOGIC ĐƠN HÀNG ==================

    // Tạo đơn: lưu DB + gửi message sang RabbitMQ
    @Transactional
    public Order createOrder(Order order) {
        Order saved = orderRepository.save(order);
        // Gửi sự kiện OrderCreated sang RabbitMQ (xử lý nền)
        orderProducer.sendOrderCreated(saved.getId());
        return saved;
    }

    @Transactional(readOnly = true)
    public Order getOrder(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order not found: " + id));
    }

    // Lấy tất cả đơn hàng dùng cho dashboard / UI
    @Transactional(readOnly = true)
    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }

    // Worker gọi để cập nhật trạng thái nền của đơn hàng
    @Transactional
    public void updateOrderStatusProcessing(Long orderId,
                                            boolean emailSent,
                                            boolean stockUpdated,
                                            boolean logWritten) {
        Order order = getOrder(orderId);

        if (emailSent) {
            order.setEmailSent(true);
        }
        if (stockUpdated) {
            order.setStockUpdated(true);
        }
        if (logWritten) {
            order.setLogWritten(true);
        }

        orderRepository.save(order);
    }

    // =============== HUỶ ĐƠN + KHÔI PHỤC LẠI KHO ===============

    @Transactional
    public Order cancelOrder(Long orderId) {
        Order order = getOrder(orderId);

        // Idempotent: nếu đã huỷ rồi thì trả luôn, không làm gì thêm
        if (order.isCancelled()) {
            return order;
        }

        // Nếu kho đã trừ thành công trước đó thì cộng lại
        if (order.isStockUpdated()
                && order.getProductId() != null
                && order.getQuantity() > 0) {

            inventoryService.increaseStock(order.getProductId(), order.getQuantity());

            // đánh dấu lại để lần sau không hiểu nhầm là đã trừ kho
            order.setStockUpdated(false);
        }

        order.setCancelled(true);
        return orderRepository.save(order);
    }

    // ================== THỐNG KÊ ĐƠN HÀNG ==================

    public Map<String, Long> getOrderStats() {
        List<Order> orders = orderRepository.findAll();

        long total = orders.size();

        long cancelled = orders.stream()
                .filter(Order::isCancelled)
                .count();

        // Đơn xử lý nền thành công: chưa bị huỷ và cả 3 cờ đều true
        long processed = orders.stream()
                .filter(o -> !o.isCancelled()
                        && o.isEmailSent()
                        && o.isStockUpdated()
                        && o.isLogWritten())
                .count();

        // Đơn bị lỗi kho: chưa bị huỷ, email + log đã làm nhưng kho không cập nhật được
        long failedStock = orders.stream()
                .filter(o -> !o.isCancelled()
                        && o.isEmailSent()
                        && !o.isStockUpdated()
                        && o.isLogWritten())
                .count();

        // Đơn đang chờ: phần còn lại
        long pending = total - processed - failedStock - cancelled;

        return Map.of(
                "totalOrders", total,
                "processedOrders", processed,
                "pendingOrders", pending,
                "failedOrders", failedStock,
                "cancelledOrders", cancelled   
        );
    }
}

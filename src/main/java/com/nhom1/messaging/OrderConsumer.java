package com.nhom1.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import com.nhom1.entity.Order;
import com.nhom1.service.InventoryService;
import com.nhom1.service.OrderService;

@Component
public class OrderConsumer {

    private static final Logger log = LoggerFactory.getLogger(OrderConsumer.class);

    private final OrderService orderService;
    private final InventoryService inventoryService;

    public OrderConsumer(OrderService orderService,
                         InventoryService inventoryService) {
        this.orderService = orderService;
        this.inventoryService = inventoryService;
    }

    @RabbitListener(queues = "${app.rabbitmq.queue}")
    public void handleOrderCreated(OrderCreatedMessage message) {
        Long orderId = message.getOrderId();
        log.info("Received OrderCreated message for orderId={}", orderId);

        Order order = orderService.getOrder(orderId);

        log.info("[EMAIL] Sending confirmation email to customer {} for order {}",
                order.getCustomerName(), order.getId());

        boolean stockOk = inventoryService.decreaseStock(order.getProductId(), order.getQuantity());
        if (stockOk) {
            log.info("[STOCK] Đã trừ kho product {} với quantity {}",
                    order.getProductId(), order.getQuantity());
        } else {
            log.warn("[STOCK] Không thể trừ kho cho product {} vì không đủ hàng",
                    order.getProductId());
        }

        log.info("[LOG] Order processed: {}", order.getId());

        orderService.updateOrderStatusProcessing(orderId, true, stockOk, true);
    }
}

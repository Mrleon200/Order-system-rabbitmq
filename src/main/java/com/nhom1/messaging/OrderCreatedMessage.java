package com.nhom1.messaging;

import java.io.Serializable;

public class OrderCreatedMessage implements Serializable {

    private Long orderId;

    public OrderCreatedMessage() {
    }

    public OrderCreatedMessage(Long orderId) {
        this.orderId = orderId;
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }
}

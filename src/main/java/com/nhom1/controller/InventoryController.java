package com.nhom1.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.nhom1.entity.Inventory;
import com.nhom1.service.InventoryService;

@RestController
@RequestMapping("/inventory")
public class InventoryController {

    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    // ===== API  =====

    // tăng kho qua query (nếu bạn vẫn đang dùng)
    @PostMapping("/increase")
    public ResponseEntity<Map<String, Object>> increase(
            @RequestParam String productId,
            @RequestParam int quantity
    ) {
        inventoryService.increaseStock(productId, quantity);
        Inventory inv = inventoryService.getOrCreateInventory(productId);
        return ResponseEntity.ok(Map.of(
                "productId", inv.getProductId(),
                "quantity", inv.getQuantity()
        ));
    }

    // xem 1 productId
    @GetMapping("/{productId}")
    public Inventory get(@PathVariable String productId) {
        return inventoryService.getOrCreateInventory(productId);
    }

    // ===== API CHO inventory.html =====

    @GetMapping
    public List<Inventory> getAllInventories() {
        return inventoryService.getAllInventories();
    }

    // DTO đơn giản nhận JSON từ FE
    public static class InventoryRequest {
        private String productId;
        private Integer quantity;

        public String getProductId() { return productId; }
        public void setProductId(String productId) { this.productId = productId; }

        public Integer getQuantity() { return quantity; }
        public void setQuantity(Integer quantity) { this.quantity = quantity; }
    }

    @PostMapping
    public Inventory createInventory(@RequestBody InventoryRequest req) {
        return inventoryService.createInventory(req.getProductId(), req.getQuantity());
    }

    @PutMapping("/{id}")
    public Inventory updateInventory(@PathVariable Long id,
                                     @RequestBody InventoryRequest req) {
        return inventoryService.updateInventory(id, req.getProductId(), req.getQuantity());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteInventory(@PathVariable Long id) {
        inventoryService.deleteInventory(id);
        return ResponseEntity.noContent().build();
    }
}

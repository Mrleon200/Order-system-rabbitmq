package com.nhom1.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nhom1.entity.Inventory;
import com.nhom1.repository.InventoryRepository;

@Service
public class InventoryService {

    private static final Logger log = LoggerFactory.getLogger(InventoryService.class);

    private final InventoryRepository inventoryRepository;

    public InventoryService(InventoryRepository inventoryRepository) {
        this.inventoryRepository = inventoryRepository;
    }

    public Inventory getOrCreateInventory(String productId) {
        return inventoryRepository.findByProductId(productId)
                .orElseGet(() -> {
                    Inventory inv = new Inventory(productId, 0);
                    Inventory saved = inventoryRepository.save(inv);
                    log.info("[INVENTORY] Tạo mới inventory cho product {} với quantity = 0", productId);
                    return saved;
                });
    }

    @Transactional
    public void increaseStock(String productId, int qty) {
        Inventory inv = getOrCreateInventory(productId);
        int before = inv.getQuantity();
        int after = before + qty;
        inv.setQuantity(after);
        inventoryRepository.save(inv);
        log.info("[INVENTORY] Tăng tồn kho product {}: {} -> {}", productId, before, after);
    }

    @Transactional
    public boolean decreaseStock(String productId, int qty) {
        Inventory inv = getOrCreateInventory(productId);
        int before = inv.getQuantity();

        if (before < qty) {
            log.warn("[INVENTORY] Không đủ tồn kho cho product {}. Hiện tại: {}, cần: {}",
                    productId, before, qty);
            return false;
        }

        int after = before - qty;
        inv.setQuantity(after);
        inventoryRepository.save(inv);

        log.info("[INVENTORY] Giảm tồn kho product {}: {} -> {}", productId, before, after);
        return true;
    }

    // Lấy toàn bộ kho
    public java.util.List<Inventory> getAllInventories() {
    return inventoryRepository.findAll();
    }

    // Tạo mới inventory (hoặc cập nhật nếu đã tồn tại productId)
    @org.springframework.transaction.annotation.Transactional
    public Inventory createInventory(String productId, int quantity) {
        return inventoryRepository.findByProductId(productId)
            .map(inv -> {
                inv.setQuantity(quantity);
                return inventoryRepository.save(inv);
            })
            .orElseGet(() -> {
                Inventory inv = new Inventory(productId, quantity);
                return inventoryRepository.save(inv);
            });
    }

    // Cập nhật inventory theo id
    @org.springframework.transaction.annotation.Transactional
    public Inventory updateInventory(Long id, String productId, int quantity) {
        Inventory inv = inventoryRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Inventory not found: " + id));
        inv.setProductId(productId);
        inv.setQuantity(quantity);
    return inventoryRepository.save(inv);
    }

    // Xoá inventory theo id
    @org.springframework.transaction.annotation.Transactional
    public void deleteInventory(Long id) {
        inventoryRepository.deleteById(id);
    }
}

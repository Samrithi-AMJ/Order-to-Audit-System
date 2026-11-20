package com.nittrichy.ordertoaudit;

import com.nittrichy.ordertoaudit.entity.Product;
import com.nittrichy.ordertoaudit.enums.MovementType;
import com.nittrichy.ordertoaudit.enums.ProductCategory;
import com.nittrichy.ordertoaudit.enums.ReferenceType;
import com.nittrichy.ordertoaudit.exception.InsufficientStockException;
import com.nittrichy.ordertoaudit.repository.ProductRepository;
import com.nittrichy.ordertoaudit.repository.StockMovementRepository;
import com.nittrichy.ordertoaudit.service.InventoryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(locations = "classpath:application-test.properties")
class InventoryConcurrencyIntegrationTest {
    @Autowired private InventoryService inventoryService;
    @Autowired private ProductRepository productRepository;
    @Autowired private StockMovementRepository stockMovementRepository;

    @Test
    void concurrentIssuesCannotOversellOrLoseStockUpdates() throws Exception {
        Product product = productRepository.save(Product.builder()
                .productCode("CONC-" + UUID.randomUUID().toString().replace("-", "").substring(0, 8))
                .name("Concurrent Stock Test")
                .category(ProductCategory.MIXTURE)
                .unit("PACKET")
                .sellingPrice(new BigDecimal("10.00"))
                .gstRate(new BigDecimal("5.00"))
                .currentStock(new BigDecimal("10.000"))
                .build());
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<Boolean> first = executor.submit(() -> issue(product, ready, start));
            Future<Boolean> second = executor.submit(() -> issue(product, ready, start));
            ready.await();
            start.countDown();

            int successfulIssues = (first.get() ? 1 : 0) + (second.get() ? 1 : 0);
            Product persisted = productRepository.findById(product.getId()).orElseThrow();

            assertEquals(1, successfulIssues);
            assertEquals(0, new BigDecimal("2.000").compareTo(persisted.getCurrentStock()));
            assertEquals(1, stockMovementRepository.findByProductIdOrderByMovementDateDesc(product.getId()).size());
        } finally {
            executor.shutdownNow();
        }
    }

    private boolean issue(Product product, CountDownLatch ready, CountDownLatch start) throws InterruptedException {
        ready.countDown();
        start.await();
        try {
            inventoryService.recordProductMovement(product, new BigDecimal("-8.000"), MovementType.SALES_DISPATCH,
                    new BigDecimal("10.00"), ReferenceType.SALES_ORDER, null, "CONCURRENT-TEST",
                    "Concurrent stock test", null);
            return true;
        } catch (InsufficientStockException expected) {
            return false;
        }
    }
}

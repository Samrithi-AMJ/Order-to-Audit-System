package com.nittrichy.ordertoaudit;

import com.nittrichy.ordertoaudit.dto.BomDto;
import com.nittrichy.ordertoaudit.dto.BomItemDto;
import com.nittrichy.ordertoaudit.dto.ProductionRunRequestDto;
import com.nittrichy.ordertoaudit.entity.BOM;
import com.nittrichy.ordertoaudit.entity.BOMItem;
import com.nittrichy.ordertoaudit.entity.Product;
import com.nittrichy.ordertoaudit.entity.RawMaterial;
import com.nittrichy.ordertoaudit.enums.ItemType;
import com.nittrichy.ordertoaudit.enums.MaterialCategory;
import com.nittrichy.ordertoaudit.enums.ProductCategory;
import com.nittrichy.ordertoaudit.exception.BusinessRuleException;
import com.nittrichy.ordertoaudit.exception.InsufficientStockException;
import com.nittrichy.ordertoaudit.repository.BOMRepository;
import com.nittrichy.ordertoaudit.repository.ProductRepository;
import com.nittrichy.ordertoaudit.repository.RawMaterialRepository;
import com.nittrichy.ordertoaudit.service.BomService;
import com.nittrichy.ordertoaudit.service.ProductionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(locations = "classpath:application-test.properties")
@Transactional
class BomProductionRulesTest {
    @Autowired private BomService bomService;
    @Autowired private ProductionService productionService;
    @Autowired private ProductRepository productRepository;
    @Autowired private RawMaterialRepository rawMaterialRepository;
    @Autowired private BOMRepository bomRepository;

    @Test
    void creatingNewBomDeactivatesPriorActiveBomForTheSameProduct() {
        Product product = product("BOM-ACTIVE");
        RawMaterial material = material("BOM-RM", "20.000");

        bomService.createBom(bomDto("BOM-A", product, material));
        bomService.createBom(bomDto("BOM-B", product, material));

        List<BOM> activeBoms = bomRepository.findAllByProductIdAndActiveTrue(product.getId());
        assertEquals(1, activeBoms.size());
        assertEquals("BOM-B", activeBoms.get(0).getBomCode());
    }

    @Test
    void productionRejectsBomOwnedByAnotherProduct() {
        Product selectedProduct = product("PROD-SELECTED");
        Product otherProduct = product("PROD-OTHER");
        RawMaterial material = material("OWNERSHIP-RM", "20.000");
        BOM otherProductsBom = bomRepository.save(BOM.builder()
                .bomCode("BOM-OWNERSHIP")
                .product(otherProduct)
                .batchSize(new BigDecimal("10.000"))
                .active(true)
                .build());
        otherProductsBom.addItem(BOMItem.builder()
                .itemType(ItemType.RAW_MATERIAL)
                .rawMaterial(material)
                .standardQuantity(BigDecimal.ONE)
                .unit("KG")
                .build());
        bomRepository.save(otherProductsBom);

        assertThrows(BusinessRuleException.class, () -> productionService.recordProductionRun(
                ProductionRunRequestDto.builder()
                        .productId(selectedProduct.getId())
                        .bomId(otherProductsBom.getId())
                        .batchMultiplier(BigDecimal.ONE)
                        .actualGoodOutput(new BigDecimal("10.000"))
                        .build()));
    }

    @Test
    void insufficientMaterialStockRollsBackProductionAndStockMovement() {
        Product product = product("PROD-INSUFFICIENT");
        RawMaterial material = material("RM-INSUFFICIENT", "1.000");
        BOM bom = bomRepository.save(BOM.builder()
                .bomCode("BOM-INSUFFICIENT")
                .product(product)
                .batchSize(new BigDecimal("10.000"))
                .active(true)
                .build());
        bom.addItem(BOMItem.builder()
                .itemType(ItemType.RAW_MATERIAL)
                .rawMaterial(material)
                .standardQuantity(new BigDecimal("2.000"))
                .unit("KG")
                .build());
        bomRepository.save(bom);

        assertThrows(InsufficientStockException.class, () -> productionService.recordProductionRun(
                ProductionRunRequestDto.builder()
                        .productId(product.getId())
                        .bomId(bom.getId())
                        .batchMultiplier(BigDecimal.ONE)
                        .actualGoodOutput(new BigDecimal("10.000"))
                        .build()));

        assertEquals(0, new BigDecimal("1.000").compareTo(
                rawMaterialRepository.findById(material.getId()).orElseThrow().getCurrentStock()));
    }

    @Test
    void bomRejectsMissingAndDuplicateComponents() {
        Product product = product("PROD-VALIDATION");
        RawMaterial material = material("RM-VALIDATION", "1.000");
        BomDto empty = BomDto.builder().bomCode("BOM-EMPTY").productId(product.getId())
                .batchSize(BigDecimal.ONE).items(List.of()).build();
        assertThrows(BusinessRuleException.class, () -> bomService.createBom(empty));

        BomItemDto duplicate = BomItemDto.builder().itemType(ItemType.RAW_MATERIAL)
                .rawMaterialId(material.getId()).standardQuantity(BigDecimal.ONE).build();
        BomDto duplicateBom = BomDto.builder().bomCode("BOM-DUPLICATE").productId(product.getId())
                .batchSize(BigDecimal.ONE).items(List.of(duplicate, duplicate)).build();
        assertThrows(BusinessRuleException.class, () -> bomService.createBom(duplicateBom));
    }

    private Product product(String prefix) {
        return productRepository.save(Product.builder()
                .productCode(prefix.substring(0, Math.min(prefix.length(), 15)) + "-" + UUID.randomUUID().toString().replace("-", "").substring(0, 8))
                .name(prefix)
                .category(ProductCategory.MIXTURE)
                .unit("PACKET")
                .sellingPrice(new BigDecimal("20.00"))
                .gstRate(new BigDecimal("5.00"))
                .currentStock(BigDecimal.ZERO)
                .build());
    }

    private RawMaterial material(String prefix, String stock) {
        return rawMaterialRepository.save(RawMaterial.builder()
                .materialCode(prefix.substring(0, Math.min(prefix.length(), 15)) + "-" + UUID.randomUUID().toString().replace("-", "").substring(0, 8))
                .name(prefix)
                .category(MaterialCategory.FLOUR)
                .unit("KG")
                .currentStock(new BigDecimal(stock))
                .weightedAvgCost(new BigDecimal("40.00"))
                .active(true)
                .build());
    }

    private BomDto bomDto(String code, Product product, RawMaterial material) {
        return BomDto.builder().bomCode(code).productId(product.getId())
                .batchSize(new BigDecimal("10.000"))
                .items(List.of(BomItemDto.builder().itemType(ItemType.RAW_MATERIAL)
                        .rawMaterialId(material.getId()).standardQuantity(BigDecimal.ONE).build()))
                .build();
    }
}

package com.nittrichy.ordertoaudit.service;

import com.nittrichy.ordertoaudit.dto.BomDto;
import com.nittrichy.ordertoaudit.dto.BomItemDto;
import com.nittrichy.ordertoaudit.entity.*;
import com.nittrichy.ordertoaudit.enums.ItemType;
import com.nittrichy.ordertoaudit.exception.BusinessRuleException;
import com.nittrichy.ordertoaudit.exception.ResourceNotFoundException;
import com.nittrichy.ordertoaudit.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BomService {

    private final BOMRepository bomRepository;
    private final BOMItemRepository bomItemRepository;
    private final ProductRepository productRepository;
    private final RawMaterialRepository rawMaterialRepository;
    private final PackagingMaterialRepository packagingMaterialRepository;

    @Transactional(readOnly = true)
    public List<BomDto> getAllBoms() {
        return bomRepository.findAll().stream().map(this::mapToDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public BomDto getBomById(Long id) {
        BOM bom = bomRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("BOM", "id", id));
        return mapToDto(bom);
    }

    @Transactional(readOnly = true)
    public BomDto getActiveBomByProductId(Long productId) {
        List<BOM> activeBoms = bomRepository.findAllByProductIdAndActiveTrue(productId);
        if (activeBoms.isEmpty()) {
            throw new ResourceNotFoundException("Active BOM for product id " + productId + " not found");
        }
        if (activeBoms.size() > 1) {
            throw new BusinessRuleException("Multiple active BOMs exist for product id " + productId);
        }
        return mapToDto(activeBoms.get(0));
    }

    @Transactional
    public BomDto createBom(BomDto dto) {
        if (bomRepository.existsByBomCode(dto.getBomCode())) {
            throw new BusinessRuleException("BOM code '" + dto.getBomCode() + "' already exists");
        }

        Product product = productRepository.findByIdForUpdate(dto.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", dto.getProductId()));

        validateBomContents(dto);
        bomRepository.findAllByProductIdAndActiveTrue(product.getId()).forEach(activeBom -> activeBom.setActive(false));

        BOM bom = BOM.builder()
                .bomCode(dto.getBomCode())
                .product(product)
                .batchSize(dto.getBatchSize())
                .description(dto.getDescription())
                .version(dto.getVersion() != null ? dto.getVersion() : 1)
                .active(true)
                .build();

        if (dto.getItems() != null) {
            for (BomItemDto itemDto : dto.getItems()) {
                BOMItem item = createBomItemFromDto(itemDto);
                bom.addItem(item);
            }
        }

        return mapToDto(bomRepository.save(bom));
    }

    @Transactional
    public BomDto updateBom(Long id, BomDto dto) {
        BOM bom = bomRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("BOM", "id", id));

        productRepository.findByIdForUpdate(bom.getProduct().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", bom.getProduct().getId()));
        validateBomContents(dto);
        if (dto.isActive()) {
            bomRepository.findAllByProductIdAndActiveTrue(bom.getProduct().getId()).stream()
                    .filter(activeBom -> !activeBom.getId().equals(bom.getId()))
                    .forEach(activeBom -> activeBom.setActive(false));
        }

        bom.setBatchSize(dto.getBatchSize());
        bom.setDescription(dto.getDescription());
        bom.setActive(dto.isActive());
        if (dto.getVersion() != null) bom.setVersion(dto.getVersion());

        // Replace items
        bom.getItems().clear();
        if (dto.getItems() != null) {
            for (BomItemDto itemDto : dto.getItems()) {
                BOMItem item = createBomItemFromDto(itemDto);
                bom.addItem(item);
            }
        }

        return mapToDto(bomRepository.save(bom));
    }

    private BOMItem createBomItemFromDto(BomItemDto itemDto) {
        if (itemDto.getItemType() == null || itemDto.getStandardQuantity() == null
                || itemDto.getStandardQuantity().compareTo(java.math.BigDecimal.ZERO) <= 0) {
            throw new BusinessRuleException("BOM item type and a positive standard quantity are required");
        }
        RawMaterial rm = null;
        PackagingMaterial pm = null;

        if (itemDto.getItemType() == ItemType.RAW_MATERIAL) {
            if (itemDto.getRawMaterialId() == null) {
                throw new BusinessRuleException("Raw material ID is required for RAW_MATERIAL BOM item");
            }
            rm = rawMaterialRepository.findById(itemDto.getRawMaterialId())
                    .orElseThrow(() -> new ResourceNotFoundException("RawMaterial", "id", itemDto.getRawMaterialId()));
            if (!rm.isActive()) throw new BusinessRuleException("Inactive raw material cannot be added to a BOM");
        } else if (itemDto.getItemType() == ItemType.PACKAGING_MATERIAL) {
            if (itemDto.getPackagingMaterialId() == null) {
                throw new BusinessRuleException("Packaging material ID is required for PACKAGING_MATERIAL BOM item");
            }
            pm = packagingMaterialRepository.findById(itemDto.getPackagingMaterialId())
                    .orElseThrow(() -> new ResourceNotFoundException("PackagingMaterial", "id", itemDto.getPackagingMaterialId()));
            if (!pm.isActive()) throw new BusinessRuleException("Inactive packaging material cannot be added to a BOM");
        } else {
            throw new BusinessRuleException("Unsupported BOM item type: " + itemDto.getItemType());
        }

        if ((rm != null && itemDto.getPackagingMaterialId() != null)
                || (pm != null && itemDto.getRawMaterialId() != null)) {
            throw new BusinessRuleException("A BOM item must reference exactly one material matching its item type");
        }

        return BOMItem.builder()
                .itemType(itemDto.getItemType())
                .rawMaterial(rm)
                .packagingMaterial(pm)
                .standardQuantity(itemDto.getStandardQuantity())
                .unit(itemDto.getUnit() != null ? itemDto.getUnit() : (rm != null ? rm.getUnit() : (pm != null ? pm.getUnit() : "")))
                .notes(itemDto.getNotes())
                .build();
    }

    private void validateBomContents(BomDto dto) {
        if (dto.getBatchSize() == null || dto.getBatchSize().compareTo(java.math.BigDecimal.ZERO) <= 0) {
            throw new BusinessRuleException("BOM batch size must be greater than zero");
        }
        if (dto.getItems() == null || dto.getItems().isEmpty()) {
            throw new BusinessRuleException("BOM must contain at least one material component");
        }
        java.util.Set<String> componentKeys = new java.util.HashSet<>();
        for (BomItemDto item : dto.getItems()) {
            createBomItemValidation(item);
            String componentId = item.getItemType() == ItemType.RAW_MATERIAL
                    ? "RM:" + item.getRawMaterialId() : "PM:" + item.getPackagingMaterialId();
            if (!componentKeys.add(componentId)) {
                throw new BusinessRuleException("BOM contains duplicate component " + componentId);
            }
        }
    }

    private void createBomItemValidation(BomItemDto item) {
        if (item.getItemType() == null || item.getStandardQuantity() == null
                || item.getStandardQuantity().compareTo(java.math.BigDecimal.ZERO) <= 0) {
            throw new BusinessRuleException("BOM item type and a positive standard quantity are required");
        }
        if (item.getItemType() == ItemType.RAW_MATERIAL && item.getRawMaterialId() == null) {
            throw new BusinessRuleException("Raw material ID is required for RAW_MATERIAL BOM item");
        }
        if (item.getItemType() == ItemType.PACKAGING_MATERIAL && item.getPackagingMaterialId() == null) {
            throw new BusinessRuleException("Packaging material ID is required for PACKAGING_MATERIAL BOM item");
        }
        if (item.getItemType() != ItemType.RAW_MATERIAL && item.getItemType() != ItemType.PACKAGING_MATERIAL) {
            throw new BusinessRuleException("Unsupported BOM item type: " + item.getItemType());
        }
        if ((item.getItemType() == ItemType.RAW_MATERIAL && item.getPackagingMaterialId() != null)
                || (item.getItemType() == ItemType.PACKAGING_MATERIAL && item.getRawMaterialId() != null)) {
            throw new BusinessRuleException("A BOM item must reference exactly one material matching its item type");
        }
    }

    public BomDto mapToDto(BOM bom) {
        List<BomItemDto> itemDtos = bom.getItems().stream().map(item -> {
            Long rmId = item.getRawMaterial() != null ? item.getRawMaterial().getId() : null;
            String rmName = item.getRawMaterial() != null ? item.getRawMaterial().getName() : null;
            String rmCode = item.getRawMaterial() != null ? item.getRawMaterial().getMaterialCode() : null;

            Long pmId = item.getPackagingMaterial() != null ? item.getPackagingMaterial().getId() : null;
            String pmName = item.getPackagingMaterial() != null ? item.getPackagingMaterial().getName() : null;
            String pmCode = item.getPackagingMaterial() != null ? item.getPackagingMaterial().getMaterialCode() : null;

            return BomItemDto.builder()
                    .id(item.getId())
                    .itemType(item.getItemType())
                    .rawMaterialId(rmId)
                    .rawMaterialName(rmName)
                    .rawMaterialCode(rmCode)
                    .packagingMaterialId(pmId)
                    .packagingMaterialName(pmName)
                    .packagingMaterialCode(pmCode)
                    .standardQuantity(item.getStandardQuantity())
                    .unit(item.getUnit())
                    .notes(item.getNotes())
                    .build();
        }).collect(Collectors.toList());

        return BomDto.builder()
                .id(bom.getId())
                .bomCode(bom.getBomCode())
                .productId(bom.getProduct().getId())
                .productName(bom.getProduct().getName())
                .productCode(bom.getProduct().getProductCode())
                .batchSize(bom.getBatchSize())
                .description(bom.getDescription())
                .version(bom.getVersion())
                .active(bom.isActive())
                .items(itemDtos)
                .build();
    }
}


package com.nittrichy.ordertoaudit;

import com.nittrichy.ordertoaudit.dto.DispatchRequestDto;
import com.nittrichy.ordertoaudit.dto.OrderApprovalDto;
import com.nittrichy.ordertoaudit.dto.SalesOrderItemDto;
import com.nittrichy.ordertoaudit.dto.SalesOrderRequestDto;
import com.nittrichy.ordertoaudit.entity.Customer;
import com.nittrichy.ordertoaudit.entity.Invoice;
import com.nittrichy.ordertoaudit.entity.Product;
import com.nittrichy.ordertoaudit.entity.SalesOrder;
import com.nittrichy.ordertoaudit.enums.ItemType;
import com.nittrichy.ordertoaudit.enums.MovementType;
import com.nittrichy.ordertoaudit.enums.ProductCategory;
import com.nittrichy.ordertoaudit.enums.ReferenceType;
import com.nittrichy.ordertoaudit.exception.BusinessRuleException;
import com.nittrichy.ordertoaudit.exception.InsufficientStockException;
import com.nittrichy.ordertoaudit.repository.CustomerRepository;
import com.nittrichy.ordertoaudit.repository.ProductRepository;
import com.nittrichy.ordertoaudit.repository.SalesOrderRepository;
import com.nittrichy.ordertoaudit.service.DispatchService;
import com.nittrichy.ordertoaudit.service.InventoryService;
import com.nittrichy.ordertoaudit.service.InvoiceService;
import com.nittrichy.ordertoaudit.service.SalesOrderService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(locations = "classpath:application-test.properties")
@Transactional
public class BusinessLogicUnitTest {

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private SalesOrderService salesOrderService;

    @Autowired
    private DispatchService dispatchService;

    @Autowired
    private InvoiceService invoiceService;

    @Autowired
    private SalesOrderRepository salesOrderRepository;

    @Test
    @DisplayName("Prevent negative inventory when attempting to issue more stock than available")
    void testNegativeInventoryPrevention() {
        Product product = productRepository.save(Product.builder()
                .productCode("PROD-TEST-01")
                .name("Test Mixture")
                .category(ProductCategory.MIXTURE)
                .unit("PACKET")
                .sellingPrice(new BigDecimal("60.00"))
                .gstRate(new BigDecimal("5.00"))
                .currentStock(new BigDecimal("10.000"))
                .build());

        // Attempting to deduct 15 packets when only 10 are available
        assertThrows(InsufficientStockException.class, () -> {
            inventoryService.recordProductMovement(
                    product,
                    new BigDecimal("-15.000"),
                    MovementType.SALES_DISPATCH,
                    new BigDecimal("60.00"),
                    ReferenceType.SALES_ORDER,
                    1L,
                    "TEST-REF",
                    "Over-deduction test",
                    null
            );
        });
    }

    @Test
    @DisplayName("Verify inter-state invoice uses the product GST rate as IGST")
    void testInterStateGstCalculation() {
        Product product = productRepository.save(Product.builder()
                .productCode("PROD-TEST-KA")
                .name("Spicy Mixture")
                .category(ProductCategory.MIXTURE)
                .unit("PACKET")
                .hsnCode("1905")
                .sellingPrice(new BigDecimal("100.00"))
                .gstRate(new BigDecimal("12.00"))
                .currentStock(new BigDecimal("50.000"))
                .build());

        // Karnataka customer (home state is Tamil Nadu)
        Customer customerKA = customerRepository.save(Customer.builder()
                .customerCode("CUST-KA-TEST")
                .name("Bengaluru Sweets")
                .state("Karnataka")
                .stateCode("29")
                .gstin("29AABCU9999K1Z5")
                .outstandingBalance(BigDecimal.ZERO)
                .build());

        SalesOrderRequestDto orderReq = SalesOrderRequestDto.builder()
                .orderNumber("SO-KA-001")
                .customerId(customerKA.getId())
                .orderDate(LocalDate.now())
                .items(List.of(
                        SalesOrderItemDto.builder()
                                .productId(product.getId())
                                .quantity(new BigDecimal("10.000"))
                                .unitPrice(new BigDecimal("100.00"))
                                .build()
                ))
                .build();

        var orderDto = salesOrderService.createSalesOrder(orderReq);
        salesOrderService.processOrderApproval(orderDto.getId(), OrderApprovalDto.builder().approved(true).build());

        // Generate invoice for order
        Invoice invoice = invoiceService.generateInvoiceForOrder(orderDto.getId());

        assertTrue(invoice.isInterState(), "Customer from Karnataka should be marked as inter-state sale");
        assertEquals(0, new BigDecimal("1000.00").compareTo(invoice.getTaxableValue()), "Taxable value: 10 * 100 = 1000.00");
        assertEquals(0, new BigDecimal("120.00").compareTo(invoice.getIgstAmount()), "IGST should use the configured 12% product rate");
        assertEquals(0, BigDecimal.ZERO.compareTo(invoice.getCgstAmount()), "CGST must be zero for inter-state sale");
        assertEquals(0, BigDecimal.ZERO.compareTo(invoice.getSgstAmount()), "SGST must be zero for inter-state sale");
        assertEquals(0, new BigDecimal("1120.00").compareTo(invoice.getGrandTotal()), "Grand total must include 12% IGST");
    }

    @Test
    @DisplayName("Verify Tamil Nadu invoice splits configured product GST equally into CGST and SGST")
    void testIntraStateGstUsesProductRate() {
        Product product = productRepository.save(Product.builder()
                .productCode("PROD-TEST-TN-GST")
                .name("Tax Rate Test Product")
                .category(ProductCategory.MIXTURE)
                .unit("PACKET")
                .sellingPrice(new BigDecimal("100.00"))
                .gstRate(new BigDecimal("12.00"))
                .currentStock(new BigDecimal("20.000"))
                .build());
        Customer customer = customerRepository.save(Customer.builder()
                .customerCode("CUST-TN-GST")
                .name("Trichy GST Test")
                .state("Tamil Nadu")
                .stateCode("33")
                .outstandingBalance(BigDecimal.ZERO)
                .build());
        var order = salesOrderService.createSalesOrder(SalesOrderRequestDto.builder()
                .orderNumber("SO-TN-GST-001")
                .customerId(customer.getId())
                .orderDate(LocalDate.now())
                .items(List.of(SalesOrderItemDto.builder()
                        .productId(product.getId())
                        .quantity(new BigDecimal("10.000"))
                        .unitPrice(new BigDecimal("100.00"))
                        .build()))
                .build());

        Invoice invoice = invoiceService.generateInvoiceForOrder(order.getId());

        assertFalse(invoice.isInterState());
        assertEquals(0, new BigDecimal("60.00").compareTo(invoice.getCgstAmount()));
        assertEquals(0, new BigDecimal("60.00").compareTo(invoice.getSgstAmount()));
        assertEquals(0, BigDecimal.ZERO.compareTo(invoice.getIgstAmount()));
        assertEquals(0, new BigDecimal("1120.00").compareTo(invoice.getGrandTotal()));
        assertEquals(0, new BigDecimal("12.00").compareTo(invoice.getItems().get(0).getGstRate()));
    }

    @Test
    @DisplayName("Prevent duplicate dispatch of the same sales order")
    void testDuplicateDispatchPrevention() {
        Product product = productRepository.save(Product.builder()
                .productCode("PROD-TEST-DISP")
                .name("Banana Chips")
                .category(ProductCategory.CHIPS)
                .unit("PACKET")
                .sellingPrice(new BigDecimal("40.00"))
                .gstRate(new BigDecimal("5.00"))
                .currentStock(new BigDecimal("100.000"))
                .build());

        Customer customer = customerRepository.save(Customer.builder()
                .customerCode("CUST-DISP-01")
                .name("Trichy Mart")
                .state("Tamil Nadu")
                .stateCode("33")
                .outstandingBalance(BigDecimal.ZERO)
                .build());

        SalesOrderRequestDto orderReq = SalesOrderRequestDto.builder()
                .orderNumber("SO-DISP-001")
                .customerId(customer.getId())
                .orderDate(LocalDate.now())
                .items(List.of(
                        SalesOrderItemDto.builder()
                                .productId(product.getId())
                                .quantity(new BigDecimal("20.000"))
                                .unitPrice(new BigDecimal("40.00"))
                                .build()
                ))
                .build();

        var orderDto = salesOrderService.createSalesOrder(orderReq);
        salesOrderService.processOrderApproval(orderDto.getId(), OrderApprovalDto.builder().approved(true).build());

        // First dispatch should succeed
        assertDoesNotThrow(() -> dispatchService.dispatchOrder(orderDto.getId(), new DispatchRequestDto()));

        // Second dispatch must fail with BusinessRuleException
        assertThrows(BusinessRuleException.class, () -> {
            dispatchService.dispatchOrder(orderDto.getId(), new DispatchRequestDto());
        });
    }
}

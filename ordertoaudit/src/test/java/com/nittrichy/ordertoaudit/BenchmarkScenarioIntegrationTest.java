package com.nittrichy.ordertoaudit;

import com.nittrichy.ordertoaudit.dto.BenchmarkResponseDto;
import com.nittrichy.ordertoaudit.entity.Customer;
import com.nittrichy.ordertoaudit.exception.BusinessRuleException;
import com.nittrichy.ordertoaudit.repository.CustomerRepository;
import com.nittrichy.ordertoaudit.service.BenchmarkService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(locations = "classpath:application-test.properties")
@Transactional
public class BenchmarkScenarioIntegrationTest {

    @Autowired
    private BenchmarkService benchmarkService;

    @Autowired
    private CustomerRepository customerRepository;

    @Test
    @DisplayName("Execute Mandatory Benchmark Scenario and Verify Exact Calculations End-to-End")
    void testMandatoryBenchmarkScenario() {
        BenchmarkResponseDto result = benchmarkService.runBenchmark();

        assertNotNull(result, "Benchmark response should not be null");
        assertTrue(result.isSuccess(), "Mandatory benchmark scenario should pass with 100% accuracy");

        // 1. Stock balances verification
        BenchmarkResponseDto.BenchmarkStockResult stocks = result.getStockBalances();
        assertNotNull(stocks);
        assertEquals(0, new BigDecimal("120.000").compareTo(stocks.getFlourClosingStock()), "Flour closing stock must be exactly 120 kg");
        assertEquals(0, new BigDecimal("19.500").compareTo(stocks.getOilClosingStock()), "Cooking oil closing stock must be exactly 19.5 litres");
        assertEquals(0, new BigDecimal("3.800").compareTo(stocks.getSpiceClosingStock()), "Spice mix closing stock must be exactly 3.8 kg");
        assertEquals(0, new BigDecimal("306.000").compareTo(stocks.getPouchClosingStock()), "Pouch closing stock must be exactly 306 pieces");
        assertEquals(0, new BigDecimal("21.000").compareTo(stocks.getCartonClosingStock()), "Carton closing stock must be exactly 21 pieces");
        assertEquals(0, new BigDecimal("238.000").compareTo(stocks.getFinishedGoodsPostProd()), "Finished Murukku post-production stock must be exactly 238 packets");
        assertEquals(0, new BigDecimal("88.000").compareTo(stocks.getFinishedGoodsPostSale()), "Finished Murukku post-Retailer A dispatch stock must be exactly 88 packets");

        // 2. Variance verification
        BenchmarkResponseDto.BenchmarkVarianceResult variance = result.getVarianceResults();
        assertNotNull(variance);
        assertEquals(0, new BigDecimal("-6.00").compareTo(variance.getYieldVariancePercentage()), "Yield variance must be exactly -6.00%");
        assertEquals(0, new BigDecimal("5.00").compareTo(variance.getOilConsumptionVariance()), "Oil consumption variance must be exactly +5.00%");
        assertTrue(variance.isYieldVarianceFlagged(), "Yield variance > 3% must be flagged");
        assertTrue(variance.isOilVarianceFlagged(), "Oil variance > 3% must be flagged");

        // 3. Retailer A Sales & GST (Intra-state)
        BenchmarkResponseDto.BenchmarkSalesResult salesA = result.getRetailerASalesResult();
        assertNotNull(salesA);
        assertEquals(0, new BigDecimal("7500.00").compareTo(salesA.getTaxableValue()), "Taxable value must be ₹7,500.00");
        assertEquals(0, new BigDecimal("187.50").compareTo(salesA.getCgstAmount()), "CGST must be ₹187.50 (2.5%)");
        assertEquals(0, new BigDecimal("187.50").compareTo(salesA.getSgstAmount()), "SGST must be ₹187.50 (2.5%)");
        assertEquals(0, BigDecimal.ZERO.compareTo(salesA.getIgstAmount()), "IGST must be ₹0.00 for intra-state sale");
        assertEquals(0, new BigDecimal("7875.00").compareTo(salesA.getInvoiceTotal()), "Invoice total must be ₹7,875.00");
        assertEquals(0, new BigDecimal("4000.00").compareTo(salesA.getPaymentReceived()), "Payment received must be ₹4,000.00");
        assertEquals(0, new BigDecimal("3875.00").compareTo(salesA.getOutstandingAmount()), "Outstanding balance must be ₹3,875.00");

        // 4. Distributor B Shortage & Dispatch Blocked
        BenchmarkResponseDto.BenchmarkDistributorBResult distB = result.getDistributorBResult();
        assertNotNull(distB);
        assertEquals(0, new BigDecimal("120.000").compareTo(distB.getOrderedQuantity()), "Ordered quantity must be 120 packets");
        assertEquals(0, new BigDecimal("88.000").compareTo(distB.getAvailableQuantity()), "Available finished stock must be 88 packets");
        assertEquals(0, new BigDecimal("32.000").compareTo(distB.getShortageQuantity()), "Shortage must be exactly 32 packets");
        assertTrue(distB.isDispatchBlocked(), "Dispatch must be strictly blocked due to stock shortage");

        // 5. Costing breakdown verification
        BenchmarkResponseDto.BenchmarkCostingResult costing = result.getCostingResult();
        assertNotNull(costing);
        assertEquals(0, new BigDecimal("1200.00").compareTo(costing.getLabourCost()), "Labour cost must be ₹1,200.00");
        assertEquals(0, new BigDecimal("600.00").compareTo(costing.getOverheadCost()), "Power/overhead cost must be ₹600.00");
        assertTrue(costing.getCostPerGoodPacket().compareTo(BigDecimal.ZERO) > 0, "Cost per good packet must be positive");
        assertTrue(costing.getGrossMarginPerPacket().compareTo(BigDecimal.ZERO) > 0, "Gross margin must be positive");
    }

    @Test
    @DisplayName("Refuse benchmark execution when business records exist and preserve them")
    void testBenchmarkDoesNotDeleteExistingBusinessData() {
        Customer existing = customerRepository.save(Customer.builder()
                .customerCode("SAFE-CUSTOMER")
                .name("Must be preserved")
                .state("Tamil Nadu")
                .stateCode("33")
                .outstandingBalance(BigDecimal.ZERO)
                .build());

        assertThrows(BusinessRuleException.class, () -> benchmarkService.runBenchmark());
        assertTrue(customerRepository.findById(existing.getId()).isPresent());
    }
}

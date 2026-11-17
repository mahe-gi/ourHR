package com.ourhr.statutory.engine;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ourhr.statutory.LwfRule;
import com.ourhr.statutory.PtSlab;
import com.ourhr.statutory.StatutoryQueryService;
import com.ourhr.statutory.StatutoryRule;
import com.ourhr.statutory.TdsTaxSlab;
import com.ourhr.statutory.engine.dto.StatutoryCalculationRequest;
import com.ourhr.statutory.engine.dto.StatutoryCalculationResult;
import com.ourhr.statutory.engine.strategies.EpfCalculatorStrategy;
import com.ourhr.statutory.engine.strategies.EsicCalculatorStrategy;
import com.ourhr.statutory.engine.strategies.LwfCalculatorStrategy;
import com.ourhr.statutory.engine.strategies.PtCalculatorStrategy;
import com.ourhr.statutory.engine.strategies.TdsProjectorStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IndianStatutoryEngineTest {

    @Mock
    private StatutoryQueryService statutoryQueryService;

    private ObjectMapper objectMapper;
    private IndianStatutoryEngineService engineService;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        EpfCalculatorStrategy epfStrategy = new EpfCalculatorStrategy(statutoryQueryService, objectMapper);
        EsicCalculatorStrategy esicStrategy = new EsicCalculatorStrategy(statutoryQueryService, objectMapper);
        PtCalculatorStrategy ptStrategy = new PtCalculatorStrategy(statutoryQueryService);
        LwfCalculatorStrategy lwfStrategy = new LwfCalculatorStrategy(statutoryQueryService);
        TdsProjectorStrategy tdsStrategy = new TdsProjectorStrategy(statutoryQueryService);

        List<StatutoryCalculationStrategy> strategies = List.of(
                epfStrategy, esicStrategy, ptStrategy, lwfStrategy, tdsStrategy
        );
        StatutoryStrategyFactory factory = new StatutoryStrategyFactory(strategies);
        engineService = new IndianStatutoryEngineService(strategies, factory);
    }

    // =========================================================================
    // Test Case 1: Dynamic EPF & EPS Splitter
    // =========================================================================
    @Nested
    @DisplayName("Test Case 1: Dynamic EPF & EPS Splitter")
    class DynamicEpfAndEpsSplitterTests {

        private void mockDefaultEpfRules() {
            when(statutoryQueryService.getEpfWageCeiling(any(LocalDate.class)))
                    .thenReturn(new BigDecimal("15000.00"));
            StatutoryRule rule15k = StatutoryRule.builder()
                    .ruleKey("EPF_WAGE_CEILING")
                    .ruleValue(new BigDecimal("15000.00"))
                    .ruleMetadata("{\"employee_rate\": 0.12, \"employer_eps_rate\": 0.0833, \"employer_epf_rate\": 0.0367, \"edli_rate\": 0.005, \"admin_rate\": 0.005}")
                    .effectiveFrom(LocalDate.of(2024, 4, 1))
                    .build();
            when(statutoryQueryService.getEffectiveRule(eq("EPF_WAGE_CEILING"), any(LocalDate.class)))
                    .thenReturn(Optional.of(rule15k));
        }

        @Test
        @DisplayName("Basic Pay INR 25,000.00 (> 15,000 ceiling): Capped at 15,000 wage base")
        void shouldCalculateEpfForBasicPayAboveCeiling() {
            mockDefaultEpfRules();

            StatutoryCalculationRequest request = StatutoryCalculationRequest.builder()
                    .employeeId(UUID.randomUUID())
                    .companyId(UUID.randomUUID())
                    .basicPay(new BigDecimal("25000.00"))
                    .daPay(BigDecimal.ZERO)
                    .earnedGross(new BigDecimal("25000.00"))
                    .payrollMonth(4)
                    .payrollYear(2026)
                    .pfOptOut(false)
                    .build();

            StatutoryCalculationResult result = engineService.calculate(request);

            assertNotNull(result.getEpf());
            assertEquals(new BigDecimal("15000.00"), result.getEpf().getWageBase());
            assertEquals(new BigDecimal("1800.00"), result.getEpf().getEmployeeContribution());
            assertEquals(new BigDecimal("75.00"), result.getEpf().getEdliContribution());
            assertEquals(new BigDecimal("75.00"), result.getEpf().getAdminCharges());
            assertEquals(new BigDecimal("1950.00"), result.getEpf().getTotalEmployerLiability());

            // Statutory split: EPS capped at 1,250.00, Employer EPF balance is 550.00
            assertEquals(new BigDecimal("1250.00"), result.getEpf().getEmployerEpsContribution());
            assertEquals(new BigDecimal("550.00"), result.getEpf().getEmployerEpfContribution());
        }

        @Test
        @DisplayName("Basic Pay INR 10,000.00 (< 15,000 ceiling): Full wage base of 10,000")
        void shouldCalculateEpfForBasicPayBelowCeiling() {
            mockDefaultEpfRules();

            StatutoryCalculationRequest request = StatutoryCalculationRequest.builder()
                    .employeeId(UUID.randomUUID())
                    .companyId(UUID.randomUUID())
                    .basicPay(new BigDecimal("10000.00"))
                    .daPay(BigDecimal.ZERO)
                    .earnedGross(new BigDecimal("10000.00"))
                    .payrollMonth(4)
                    .payrollYear(2026)
                    .pfOptOut(false)
                    .build();

            StatutoryCalculationResult result = engineService.calculate(request);

            assertNotNull(result.getEpf());
            assertEquals(new BigDecimal("10000.00"), result.getEpf().getWageBase());
            assertEquals(new BigDecimal("1200.00"), result.getEpf().getEmployeeContribution());
            assertEquals(new BigDecimal("833.00"), result.getEpf().getEmployerEpsContribution());
            assertEquals(new BigDecimal("367.00"), result.getEpf().getEmployerEpfContribution());
            assertEquals(new BigDecimal("50.00"), result.getEpf().getEdliContribution());
            assertEquals(new BigDecimal("50.00"), result.getEpf().getAdminCharges());
            assertEquals(new BigDecimal("1300.00"), result.getEpf().getTotalEmployerLiability());
        }

        @Test
        @DisplayName("PF Opt-Out (pfOptOut = true): All EPF contributions return 0.00")
        void shouldReturnZeroForPfOptOut() {
            StatutoryCalculationRequest request = StatutoryCalculationRequest.builder()
                    .employeeId(UUID.randomUUID())
                    .companyId(UUID.randomUUID())
                    .basicPay(new BigDecimal("35000.00"))
                    .daPay(BigDecimal.ZERO)
                    .earnedGross(new BigDecimal("50000.00"))
                    .payrollMonth(4)
                    .payrollYear(2026)
                    .pfOptOut(true)
                    .build();

            StatutoryCalculationResult result = engineService.calculate(request);

            assertNotNull(result.getEpf());
            assertEquals(new BigDecimal("0.00"), result.getEpf().getWageBase());
            assertEquals(new BigDecimal("0.00"), result.getEpf().getEmployeeContribution());
            assertEquals(new BigDecimal("0.00"), result.getEpf().getEmployerEpsContribution());
            assertEquals(new BigDecimal("0.00"), result.getEpf().getEmployerEpfContribution());
            assertEquals(new BigDecimal("0.00"), result.getEpf().getEdliContribution());
            assertEquals(new BigDecimal("0.00"), result.getEpf().getAdminCharges());
            assertEquals(new BigDecimal("0.00"), result.getEpf().getTotalEmployerLiability());
        }
    }

    // =========================================================================
    // Test Case 2: ESIC 6-Month Contribution Window Continuation Rule
    // =========================================================================
    @Nested
    @DisplayName("Test Case 2: ESIC 6-Month Contribution Window Continuation Rule")
    class EsicContinuationRuleTests {

        private void mockDefaultEsicRules() {
            StatutoryRule esicRule = StatutoryRule.builder()
                    .ruleKey("ESIC_WAGE_CEILING")
                    .ruleValue(new BigDecimal("21000.00"))
                    .ruleMetadata("{\"employee_rate\": 0.0075, \"employer_rate\": 0.0325, \"pwd_ceiling\": 25000.00}")
                    .effectiveFrom(LocalDate.of(2024, 4, 1))
                    .build();
            when(statutoryQueryService.getEffectiveRule(eq("ESIC_WAGE_CEILING"), any(LocalDate.class)))
                    .thenReturn(Optional.of(esicRule));
        }

        @Test
        @DisplayName("Initial wage in April <= 21,000 ceiling; in July wage rises to 26,000 -> ESIC continues")
        void shouldContinueEsicDeductionWhenWageExceedsCeilingInWindow() {
            mockDefaultEsicRules();

            StatutoryCalculationRequest request = StatutoryCalculationRequest.builder()
                    .employeeId(UUID.randomUUID())
                    .companyId(UUID.randomUUID())
                    .initialGrossInWindow(new BigDecimal("20000.00"))
                    .earnedGross(new BigDecimal("26000.00"))
                    .payrollMonth(7) // July
                    .payrollYear(2026)
                    .isPwd(false)
                    .build();

            StatutoryCalculationResult result = engineService.calculate(request);

            assertNotNull(result.getEsic());
            assertTrue(result.getEsic().isEligible());
            assertEquals(new BigDecimal("26000.00"), result.getEsic().getWageBase());
            assertEquals(new BigDecimal("195.00"), result.getEsic().getEmployeeContribution());
            assertEquals(new BigDecimal("845.00"), result.getEsic().getEmployerContribution());
            assertEquals(new BigDecimal("1040.00"), result.getEsic().getTotalLiability());
            assertEquals("Covered under 6-month contribution window continuation rule", result.getEsic().getNote());
        }

        @Test
        @DisplayName("Joiner with initial gross INR 26,000.00: Not eligible (0.00 contributions)")
        void shouldBeIneligibleWhenInitialGrossExceedsCeiling() {
            mockDefaultEsicRules();

            StatutoryCalculationRequest request = StatutoryCalculationRequest.builder()
                    .employeeId(UUID.randomUUID())
                    .companyId(UUID.randomUUID())
                    .initialGrossInWindow(new BigDecimal("26000.00"))
                    .earnedGross(new BigDecimal("26000.00"))
                    .payrollMonth(7)
                    .payrollYear(2026)
                    .isPwd(false)
                    .build();

            StatutoryCalculationResult result = engineService.calculate(request);

            assertNotNull(result.getEsic());
            assertFalse(result.getEsic().isEligible());
            assertEquals(new BigDecimal("0.00"), result.getEsic().getWageBase());
            assertEquals(new BigDecimal("0.00"), result.getEsic().getEmployeeContribution());
            assertEquals(new BigDecimal("0.00"), result.getEsic().getEmployerContribution());
            assertEquals(new BigDecimal("0.00"), result.getEsic().getTotalLiability());
        }

        @Test
        @DisplayName("PwD employee with gross INR 24,000.00: Eligible under enhanced INR 25,000.00 ceiling")
        void shouldBeEligibleForPwdUnderEnhancedCeiling() {
            mockDefaultEsicRules();

            StatutoryCalculationRequest request = StatutoryCalculationRequest.builder()
                    .employeeId(UUID.randomUUID())
                    .companyId(UUID.randomUUID())
                    .earnedGross(new BigDecimal("24000.00"))
                    .payrollMonth(5)
                    .payrollYear(2026)
                    .isPwd(true)
                    .build();

            StatutoryCalculationResult result = engineService.calculate(request);

            assertNotNull(result.getEsic());
            assertTrue(result.getEsic().isEligible());
            assertEquals(new BigDecimal("24000.00"), result.getEsic().getWageBase());
            assertEquals(new BigDecimal("180.00"), result.getEsic().getEmployeeContribution());
            assertEquals(new BigDecimal("780.00"), result.getEsic().getEmployerContribution());
            assertEquals(new BigDecimal("960.00"), result.getEsic().getTotalLiability());
        }
    }

    // =========================================================================
    // Test Case 3: Maharashtra PT February Rule & KA / TG Lookups
    // =========================================================================
    @Nested
    @DisplayName("Test Case 3: Maharashtra PT February Rule & Multi-State Lookups")
    class ProfessionalTaxTests {

        @Test
        @DisplayName("Maharashtra PT: Gross INR 30,000 in January (month 1) -> PT = 200.00")
        void shouldCalculateMhPtForJanuary() {
            PtSlab mhSlab = PtSlab.builder()
                    .stateCode("MH")
                    .gender("MALE")
                    .salaryFrom(new BigDecimal("10000.01"))
                    .salaryTo(null)
                    .taxAmount(new BigDecimal("200.00"))
                    .specialMonth(2)
                    .specialMonthTaxAmount(new BigDecimal("300.00"))
                    .effectiveFrom(LocalDate.of(2024, 4, 1))
                    .build();

            when(statutoryQueryService.getPtSlab(eq("MH"), eq("MALE"), eq(new BigDecimal("30000.00")), any(LocalDate.class)))
                    .thenReturn(Optional.of(mhSlab));

            StatutoryCalculationRequest request = StatutoryCalculationRequest.builder()
                    .employeeId(UUID.randomUUID())
                    .companyId(UUID.randomUUID())
                    .branchStateCode("MH")
                    .gender("MALE")
                    .earnedGross(new BigDecimal("30000.00"))
                    .payrollMonth(1) // January
                    .payrollYear(2026)
                    .build();

            StatutoryCalculationResult result = engineService.calculate(request);

            assertNotNull(result.getPt());
            assertEquals("MH", result.getPt().getStateCode());
            assertEquals(new BigDecimal("200.00"), result.getPt().getTaxAmount());
            assertFalse(result.getPt().isSpecialMonth());
        }

        @Test
        @DisplayName("Maharashtra PT: Gross INR 30,000 in February (month 2) -> PT = 300.00")
        void shouldCalculateMhPtForFebruarySpecialMonth() {
            PtSlab mhSlab = PtSlab.builder()
                    .stateCode("MH")
                    .gender("MALE")
                    .salaryFrom(new BigDecimal("10000.01"))
                    .salaryTo(null)
                    .taxAmount(new BigDecimal("200.00"))
                    .specialMonth(2)
                    .specialMonthTaxAmount(new BigDecimal("300.00"))
                    .effectiveFrom(LocalDate.of(2024, 4, 1))
                    .build();

            when(statutoryQueryService.getPtSlab(eq("MH"), eq("MALE"), eq(new BigDecimal("30000.00")), any(LocalDate.class)))
                    .thenReturn(Optional.of(mhSlab));

            StatutoryCalculationRequest request = StatutoryCalculationRequest.builder()
                    .employeeId(UUID.randomUUID())
                    .companyId(UUID.randomUUID())
                    .branchStateCode("MH")
                    .gender("MALE")
                    .earnedGross(new BigDecimal("30000.00"))
                    .payrollMonth(2) // February
                    .payrollYear(2026)
                    .build();

            StatutoryCalculationResult result = engineService.calculate(request);

            assertNotNull(result.getPt());
            assertEquals("MH", result.getPt().getStateCode());
            assertEquals(new BigDecimal("300.00"), result.getPt().getTaxAmount());
            assertTrue(result.getPt().isSpecialMonth());
        }

        @Test
        @DisplayName("Karnataka PT: Gross INR 30,000 -> PT = 200.00")
        void shouldCalculateKarnatakaPt() {
            PtSlab kaSlab = PtSlab.builder()
                    .stateCode("KA")
                    .gender("ALL")
                    .salaryFrom(new BigDecimal("15000.00"))
                    .salaryTo(null)
                    .taxAmount(new BigDecimal("200.00"))
                    .effectiveFrom(LocalDate.of(2024, 4, 1))
                    .build();

            when(statutoryQueryService.getPtSlab(eq("KA"), eq("ALL"), eq(new BigDecimal("30000.00")), any(LocalDate.class)))
                    .thenReturn(Optional.of(kaSlab));

            StatutoryCalculationRequest request = StatutoryCalculationRequest.builder()
                    .employeeId(UUID.randomUUID())
                    .companyId(UUID.randomUUID())
                    .branchStateCode("KA")
                    .gender("ALL")
                    .earnedGross(new BigDecimal("30000.00"))
                    .payrollMonth(5)
                    .payrollYear(2026)
                    .build();

            StatutoryCalculationResult result = engineService.calculate(request);

            assertNotNull(result.getPt());
            assertEquals("KA", result.getPt().getStateCode());
            assertEquals(new BigDecimal("200.00"), result.getPt().getTaxAmount());
            assertFalse(result.getPt().isSpecialMonth());
        }

        @Test
        @DisplayName("Telangana PT: Gross INR 18,000 -> PT = 150.00")
        void shouldCalculateTelanganaPt() {
            PtSlab tgSlab = PtSlab.builder()
                    .stateCode("TG")
                    .gender("ALL")
                    .salaryFrom(new BigDecimal("15000.01"))
                    .salaryTo(new BigDecimal("20000.00"))
                    .taxAmount(new BigDecimal("150.00"))
                    .effectiveFrom(LocalDate.of(2024, 4, 1))
                    .build();

            when(statutoryQueryService.getPtSlab(eq("TG"), eq("ALL"), eq(new BigDecimal("18000.00")), any(LocalDate.class)))
                    .thenReturn(Optional.of(tgSlab));

            StatutoryCalculationRequest request = StatutoryCalculationRequest.builder()
                    .employeeId(UUID.randomUUID())
                    .companyId(UUID.randomUUID())
                    .branchStateCode("TG")
                    .gender("ALL")
                    .earnedGross(new BigDecimal("18000.00"))
                    .payrollMonth(5)
                    .payrollYear(2026)
                    .build();

            StatutoryCalculationResult result = engineService.calculate(request);

            assertNotNull(result.getPt());
            assertEquals("TG", result.getPt().getStateCode());
            assertEquals(new BigDecimal("150.00"), result.getPt().getTaxAmount());
            assertFalse(result.getPt().isSpecialMonth());
        }
    }

    // =========================================================================
    // Test Case 4: Labour Welfare Fund (LWF) Seasonal Deduction
    // =========================================================================
    @Nested
    @DisplayName("Test Case 4: Labour Welfare Fund (LWF) Seasonal Deduction")
    class LwfSeasonalDeductionTests {

        @Test
        @DisplayName("Karnataka LWF: Month 12 applies 20.00 / 40.00; Month 6 applies 0.00")
        void shouldApplyKarnatakaLwfInDecemberOnly() {
            LwfRule kaRule = LwfRule.builder()
                    .stateCode("KA")
                    .employeeContribution(new BigDecimal("20.00"))
                    .employerContribution(new BigDecimal("40.00"))
                    .deductionFrequency("ANNUAL")
                    .deductionMonths(List.of(12))
                    .effectiveFrom(LocalDate.of(2024, 4, 1))
                    .build();

            when(statutoryQueryService.getLwfRule(eq("KA"), any(LocalDate.class)))
                    .thenReturn(Optional.of(kaRule));

            // Month 12 (December)
            StatutoryCalculationRequest reqDec = StatutoryCalculationRequest.builder()
                    .employeeId(UUID.randomUUID())
                    .companyId(UUID.randomUUID())
                    .branchStateCode("KA")
                    .payrollMonth(12)
                    .payrollYear(2026)
                    .build();

            StatutoryCalculationResult resDec = engineService.calculate(reqDec);

            assertNotNull(resDec.getLwf());
            assertTrue(resDec.getLwf().isApplicable());
            assertEquals(new BigDecimal("20.00"), resDec.getLwf().getEmployeeContribution());
            assertEquals(new BigDecimal("40.00"), resDec.getLwf().getEmployerContribution());
            assertEquals(new BigDecimal("60.00"), resDec.getLwf().getTotalContribution());

            // Month 6 (June)
            StatutoryCalculationRequest reqJune = StatutoryCalculationRequest.builder()
                    .employeeId(UUID.randomUUID())
                    .companyId(UUID.randomUUID())
                    .branchStateCode("KA")
                    .payrollMonth(6)
                    .payrollYear(2026)
                    .build();

            StatutoryCalculationResult resJune = engineService.calculate(reqJune);

            assertNotNull(resJune.getLwf());
            assertFalse(resJune.getLwf().isApplicable());
            assertEquals(new BigDecimal("0.00"), resJune.getLwf().getEmployeeContribution());
            assertEquals(new BigDecimal("0.00"), resJune.getLwf().getEmployerContribution());
            assertEquals(new BigDecimal("0.00"), resJune.getLwf().getTotalContribution());
        }

        @Test
        @DisplayName("Maharashtra LWF: Month 6 and Month 12 apply 12.00 / 36.00; Month 3 applies 0.00")
        void shouldApplyMaharashtraLwfInJuneAndDecember() {
            LwfRule mhRule = LwfRule.builder()
                    .stateCode("MH")
                    .employeeContribution(new BigDecimal("12.00"))
                    .employerContribution(new BigDecimal("36.00"))
                    .deductionFrequency("HALF_YEARLY")
                    .deductionMonths(List.of(6, 12))
                    .effectiveFrom(LocalDate.of(2024, 4, 1))
                    .build();

            when(statutoryQueryService.getLwfRule(eq("MH"), any(LocalDate.class)))
                    .thenReturn(Optional.of(mhRule));

            // Month 6 (June)
            StatutoryCalculationRequest reqJune = StatutoryCalculationRequest.builder()
                    .employeeId(UUID.randomUUID())
                    .companyId(UUID.randomUUID())
                    .branchStateCode("MH")
                    .payrollMonth(6)
                    .payrollYear(2026)
                    .build();

            StatutoryCalculationResult resJune = engineService.calculate(reqJune);

            assertNotNull(resJune.getLwf());
            assertTrue(resJune.getLwf().isApplicable());
            assertEquals(new BigDecimal("12.00"), resJune.getLwf().getEmployeeContribution());
            assertEquals(new BigDecimal("36.00"), resJune.getLwf().getEmployerContribution());
            assertEquals(new BigDecimal("48.00"), resJune.getLwf().getTotalContribution());

            // Month 12 (December)
            StatutoryCalculationRequest reqDec = StatutoryCalculationRequest.builder()
                    .employeeId(UUID.randomUUID())
                    .companyId(UUID.randomUUID())
                    .branchStateCode("MH")
                    .payrollMonth(12)
                    .payrollYear(2026)
                    .build();

            StatutoryCalculationResult resDec = engineService.calculate(reqDec);

            assertNotNull(resDec.getLwf());
            assertTrue(resDec.getLwf().isApplicable());
            assertEquals(new BigDecimal("12.00"), resDec.getLwf().getEmployeeContribution());
            assertEquals(new BigDecimal("36.00"), resDec.getLwf().getEmployerContribution());

            // Month 3 (March)
            StatutoryCalculationRequest reqMarch = StatutoryCalculationRequest.builder()
                    .employeeId(UUID.randomUUID())
                    .companyId(UUID.randomUUID())
                    .branchStateCode("MH")
                    .payrollMonth(3)
                    .payrollYear(2026)
                    .build();

            StatutoryCalculationResult resMarch = engineService.calculate(reqMarch);

            assertNotNull(resMarch.getLwf());
            assertFalse(resMarch.getLwf().isApplicable());
            assertEquals(new BigDecimal("0.00"), resMarch.getLwf().getEmployeeContribution());
            assertEquals(new BigDecimal("0.00"), resMarch.getLwf().getEmployerContribution());
        }
    }

    // =========================================================================
    // Test Case 5: Progressive TDS Old vs New Regime & Form 12B Integration
    // =========================================================================
    @Nested
    @DisplayName("Test Case 5: Progressive TDS Old vs New Regime & Form 12B")
    class ProgressiveTdsAndForm12BTests {

        private List<TdsTaxSlab> buildNewRegimeSlabs() {
            return List.of(
                    TdsTaxSlab.builder().financialYear("2026-2027").regime("NEW").incomeFrom(new BigDecimal("0.00")).incomeTo(new BigDecimal("300000.00")).taxRate(BigDecimal.ZERO).standardDeduction(new BigDecimal("75000.00")).rebate87aLimit(new BigDecimal("1275000.00")).build(),
                    TdsTaxSlab.builder().financialYear("2026-2027").regime("NEW").incomeFrom(new BigDecimal("300000.01")).incomeTo(new BigDecimal("700000.00")).taxRate(new BigDecimal("5.00")).standardDeduction(new BigDecimal("75000.00")).rebate87aLimit(new BigDecimal("1275000.00")).build(),
                    TdsTaxSlab.builder().financialYear("2026-2027").regime("NEW").incomeFrom(new BigDecimal("700000.01")).incomeTo(new BigDecimal("1000000.00")).taxRate(new BigDecimal("10.00")).standardDeduction(new BigDecimal("75000.00")).rebate87aLimit(new BigDecimal("1275000.00")).build(),
                    TdsTaxSlab.builder().financialYear("2026-2027").regime("NEW").incomeFrom(new BigDecimal("1000000.01")).incomeTo(new BigDecimal("1200000.00")).taxRate(new BigDecimal("15.00")).standardDeduction(new BigDecimal("75000.00")).rebate87aLimit(new BigDecimal("1275000.00")).build(),
                    TdsTaxSlab.builder().financialYear("2026-2027").regime("NEW").incomeFrom(new BigDecimal("1200000.01")).incomeTo(new BigDecimal("1500000.00")).taxRate(new BigDecimal("20.00")).standardDeduction(new BigDecimal("75000.00")).rebate87aLimit(new BigDecimal("1275000.00")).build(),
                    TdsTaxSlab.builder().financialYear("2026-2027").regime("NEW").incomeFrom(new BigDecimal("1500000.01")).incomeTo(null).taxRate(new BigDecimal("30.00")).standardDeduction(new BigDecimal("75000.00")).rebate87aLimit(new BigDecimal("1275000.00")).build()
            );
        }

        private List<TdsTaxSlab> buildOldRegimeSlabs() {
            return List.of(
                    TdsTaxSlab.builder().financialYear("2026-2027").regime("OLD").incomeFrom(new BigDecimal("0.00")).incomeTo(new BigDecimal("250000.00")).taxRate(BigDecimal.ZERO).standardDeduction(new BigDecimal("50000.00")).rebate87aLimit(new BigDecimal("500000.00")).build(),
                    TdsTaxSlab.builder().financialYear("2026-2027").regime("OLD").incomeFrom(new BigDecimal("250000.01")).incomeTo(new BigDecimal("500000.00")).taxRate(new BigDecimal("5.00")).standardDeduction(new BigDecimal("50000.00")).rebate87aLimit(new BigDecimal("500000.00")).build(),
                    TdsTaxSlab.builder().financialYear("2026-2027").regime("OLD").incomeFrom(new BigDecimal("500000.01")).incomeTo(new BigDecimal("1000000.00")).taxRate(new BigDecimal("20.00")).standardDeduction(new BigDecimal("50000.00")).rebate87aLimit(new BigDecimal("500000.00")).build(),
                    TdsTaxSlab.builder().financialYear("2026-2027").regime("OLD").incomeFrom(new BigDecimal("1000000.01")).incomeTo(null).taxRate(new BigDecimal("30.00")).standardDeduction(new BigDecimal("50000.00")).rebate87aLimit(new BigDecimal("500000.00")).build()
            );
        }

        @Test
        @DisplayName("New Regime: Standard deduction INR 75,000 and Section 87A rebate applies within 1,275,000 threshold")
        void shouldApplyNewRegimeStandardDeductionAndRebate() {
            when(statutoryQueryService.getTdsSlabs("2026-2027", "NEW"))
                    .thenReturn(buildNewRegimeSlabs());

            // Earned gross 100,000 in April (12 months remain). Projected gross = 1,200,000.
            // Taxable = 1,200,000 - 75,000 = 1,125,000 <= 1,275,000 limit -> Rebate covers full tax!
            StatutoryCalculationRequest request = StatutoryCalculationRequest.builder()
                    .employeeId(UUID.randomUUID())
                    .companyId(UUID.randomUUID())
                    .earnedGross(new BigDecimal("100000.00"))
                    .payrollMonth(4)
                    .payrollYear(2026)
                    .financialYear("2026-2027")
                    .taxRegime("NEW")
                    .build();

            StatutoryCalculationResult result = engineService.calculate(request);

            assertNotNull(result.getTds());
            assertEquals(new BigDecimal("1200000.00"), result.getTds().getProjectedAnnualGross());
            assertEquals(new BigDecimal("75000.00"), result.getTds().getStandardDeduction());
            assertEquals(new BigDecimal("1125000.00"), result.getTds().getTaxableIncome());
            assertEquals(new BigDecimal("68750.00"), result.getTds().getTaxBeforeCess());
            assertEquals(new BigDecimal("68750.00"), result.getTds().getSection87aRebate());
            assertEquals(new BigDecimal("0.00"), result.getTds().getTaxAfterRebate());
            assertEquals(new BigDecimal("0.00"), result.getTds().getTotalAnnualTax());
            assertEquals(new BigDecimal("0.00"), result.getTds().getMonthlyTdsDeduction());
        }

        @Test
        @DisplayName("Old Regime: Standard deduction INR 50,000 and calculation without 87A rebate when exceeding 500,000")
        void shouldApplyOldRegimeStandardDeductionAndComputeTax() {
            when(statutoryQueryService.getTdsSlabs("2026-2027", "OLD"))
                    .thenReturn(buildOldRegimeSlabs());

            // Earned gross 100,000 in April (12 months remain). Projected gross = 1,200,000.
            // Taxable = 1,200,000 - 50,000 = 1,150,000.
            // Tax before cess = 12,500 (250k-500k @ 5%) + 100,000 (500k-1M @ 20%) + 45,000 (150k @ 30%) = 157,500.00.
            // Cess (4%) = 6,300.00. Total annual tax = 163,800.00.
            // Monthly TDS = 163,800 / 12 = 13,650.00.
            StatutoryCalculationRequest request = StatutoryCalculationRequest.builder()
                    .employeeId(UUID.randomUUID())
                    .companyId(UUID.randomUUID())
                    .earnedGross(new BigDecimal("100000.00"))
                    .payrollMonth(4)
                    .payrollYear(2026)
                    .financialYear("2026-2027")
                    .taxRegime("OLD")
                    .build();

            StatutoryCalculationResult result = engineService.calculate(request);

            assertNotNull(result.getTds());
            assertEquals(new BigDecimal("1200000.00"), result.getTds().getProjectedAnnualGross());
            assertEquals(new BigDecimal("50000.00"), result.getTds().getStandardDeduction());
            assertEquals(new BigDecimal("1150000.00"), result.getTds().getTaxableIncome());
            assertEquals(new BigDecimal("1575000.00").divide(BigDecimal.TEN), result.getTds().getTaxBeforeCess());
            assertEquals(new BigDecimal("0.00"), result.getTds().getSection87aRebate());
            assertEquals(new BigDecimal("157500.00"), result.getTds().getTaxAfterRebate());
            assertEquals(new BigDecimal("6300.00"), result.getTds().getEducationCess());
            assertEquals(new BigDecimal("163800.00"), result.getTds().getTotalAnnualTax());
            assertEquals(new BigDecimal("13650.00"), result.getTds().getMonthlyTdsDeduction());
        }

        @Test
        @DisplayName("Form 12B Integration: Aggregates previous employer gross and TDS deduction")
        void shouldIntegrateForm12BPreviousIncomeAndTds() {
            when(statutoryQueryService.getTdsSlabs("2026-2027", "NEW"))
                    .thenReturn(buildNewRegimeSlabs());

            // Mid-year hire in Month 10 (January - 3 remaining months: Jan, Feb, Mar).
            // Current month earned gross: 150,000.00.
            // YTD in current company: 300,000.00 gross, 20,000.00 TDS paid.
            // Form 12B previous employer: 600,000.00 gross, 30,000.00 TDS paid.
            // Projected annual gross = (150,000 * 3) + 300,000 + 600,000 = 1,350,000.00.
            // Taxable = 1,350,000 - 75,000 = 1,275,000.00.
            // Since 1,275,000 <= 1,275,000 rebate limit: 87A applies! Tax = 0.
            StatutoryCalculationRequest request = StatutoryCalculationRequest.builder()
                    .employeeId(UUID.randomUUID())
                    .companyId(UUID.randomUUID())
                    .earnedGross(new BigDecimal("150000.00"))
                    .ytdEarnedGross(new BigDecimal("300000.00"))
                    .ytdTdsDeducted(new BigDecimal("20000.00"))
                    .previousEmployerGross(new BigDecimal("600000.00"))
                    .previousEmployerTds(new BigDecimal("30000.00"))
                    .payrollMonth(1) // January (3 remaining months)
                    .payrollYear(2027)
                    .financialYear("2026-2027")
                    .taxRegime("NEW")
                    .build();

            StatutoryCalculationResult result = engineService.calculate(request);

            assertNotNull(result.getTds());
            assertEquals(new BigDecimal("1350000.00"), result.getTds().getProjectedAnnualGross());
            assertEquals(new BigDecimal("75000.00"), result.getTds().getStandardDeduction());
            assertEquals(new BigDecimal("1275000.00"), result.getTds().getTaxableIncome());
            assertEquals(new BigDecimal("50000.00"), result.getTds().getTaxPaidSoFar());
            assertEquals(3, result.getTds().getRemainingMonths());
            // Since taxable income is exactly at rebate threshold (1,275,000.00), rebate applies
            assertEquals(new BigDecimal("0.00"), result.getTds().getTotalAnnualTax());
            assertEquals(new BigDecimal("0.00"), result.getTds().getMonthlyTdsDeduction());
        }
    }
}

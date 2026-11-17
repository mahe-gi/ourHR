package com.ourhr.statutory;

import com.ourhr.statutory.dto.LwfRuleDto;
import com.ourhr.statutory.dto.PtSlabDto;
import com.ourhr.statutory.dto.StatutoryRuleDto;
import com.ourhr.statutory.dto.TdsTaxSlabDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StatutoryQueryServiceTest {

    @Mock
    private StatutoryRuleRepository statutoryRuleRepository;

    @Mock
    private PtSlabRepository ptSlabRepository;

    @Mock
    private LwfRuleRepository lwfRuleRepository;

    @Mock
    private TdsTaxSlabRepository tdsTaxSlabRepository;

    @InjectMocks
    private StatutoryQueryService statutoryQueryService;

    private LocalDate date2024;
    private LocalDate date2025;
    private LocalDate date2026;

    @BeforeEach
    void setUp() {
        date2024 = LocalDate.of(2024, 6, 1);
        date2025 = LocalDate.of(2025, 4, 1);
        date2026 = LocalDate.of(2026, 4, 1);
    }

    @Nested
    @DisplayName("EPF Wage Ceiling Tests")
    class EpfWageCeilingTests {

        @Test
        @DisplayName("getEpfWageCeiling should return INR 15,000 for FY 2024-2025")
        void shouldReturnStandardEpfWageCeiling() {
            StatutoryRule rule15k = StatutoryRule.builder()
                    .id(UUID.randomUUID())
                    .ruleKey("EPF_WAGE_CEILING")
                    .ruleValue(new BigDecimal("15000.00"))
                    .ruleMetadata("{\"employee_rate\": 0.12, \"employer_eps_rate\": 0.0833, \"employer_epf_rate\": 0.0367, \"edli_rate\": 0.005, \"admin_rate\": 0.005}")
                    .effectiveFrom(LocalDate.of(2024, 4, 1))
                    .effectiveTo(null)
                    .description("Standard statutory EPF monthly wage ceiling of INR 15,000")
                    .build();

            when(statutoryRuleRepository.findEffectiveRule("EPF_WAGE_CEILING", date2024))
                    .thenReturn(Optional.of(rule15k));

            BigDecimal ceiling = statutoryQueryService.getEpfWageCeiling(date2024);

            assertEquals(new BigDecimal("15000.00"), ceiling);
            verify(statutoryRuleRepository).findEffectiveRule("EPF_WAGE_CEILING", date2024);
        }

        @Test
        @DisplayName("getEpfWageCeiling should return INR 25,000 when enhanced limit is configured")
        void shouldReturnEnhancedEpfWageCeilingOnEffectiveDate() {
            StatutoryRule rule25k = StatutoryRule.builder()
                    .id(UUID.randomUUID())
                    .ruleKey("EPF_WAGE_CEILING")
                    .ruleValue(new BigDecimal("25000.00"))
                    .ruleMetadata("{\"employee_rate\": 0.12, \"employer_eps_rate\": 0.0833, \"employer_epf_rate\": 0.0367, \"edli_rate\": 0.005, \"admin_rate\": 0.005}")
                    .effectiveFrom(LocalDate.of(2025, 4, 1))
                    .effectiveTo(null)
                    .description("Enhanced EPF monthly wage ceiling of INR 25,000")
                    .build();

            when(statutoryRuleRepository.findEffectiveRule("EPF_WAGE_CEILING", date2025))
                    .thenReturn(Optional.of(rule25k));

            BigDecimal ceiling = statutoryQueryService.getEpfWageCeiling(date2025);

            assertEquals(new BigDecimal("25000.00"), ceiling);
            verify(statutoryRuleRepository).findEffectiveRule("EPF_WAGE_CEILING", date2025);
        }

        @Test
        @DisplayName("getRuleValue should resolve enhanced ceiling by explicit rule key EPF_WAGE_CEILING_ENHANCED")
        void shouldResolveEnhancedCeilingByExplicitKey() {
            StatutoryRule enhancedRule = StatutoryRule.builder()
                    .id(UUID.randomUUID())
                    .ruleKey("EPF_WAGE_CEILING_ENHANCED")
                    .ruleValue(new BigDecimal("25000.00"))
                    .effectiveFrom(LocalDate.of(2025, 4, 1))
                    .effectiveTo(null)
                    .description("Proposed enhanced EPF monthly wage ceiling")
                    .build();

            when(statutoryRuleRepository.findEffectiveRule("EPF_WAGE_CEILING_ENHANCED", date2025))
                    .thenReturn(Optional.of(enhancedRule));

            BigDecimal ceiling = statutoryQueryService.getRuleValue("EPF_WAGE_CEILING_ENHANCED", date2025);

            assertEquals(new BigDecimal("25000.00"), ceiling);
        }

        @Test
        @DisplayName("getEpfWageCeiling should throw IllegalStateException when rule is not configured")
        void shouldThrowWhenEpfRuleNotFound() {
            LocalDate unconfiguredDate = LocalDate.of(2020, 1, 1);
            when(statutoryRuleRepository.findEffectiveRule("EPF_WAGE_CEILING", unconfiguredDate))
                    .thenReturn(Optional.empty());

            assertThrows(IllegalStateException.class,
                    () -> statutoryQueryService.getEpfWageCeiling(unconfiguredDate));
        }

        @Test
        @DisplayName("getEffectiveRule should map to StatutoryRuleDto accurately")
        void shouldMapStatutoryRuleToDto() {
            UUID ruleId = UUID.randomUUID();
            StatutoryRule rule = StatutoryRule.builder()
                    .id(ruleId)
                    .ruleKey("EPF_WAGE_CEILING")
                    .ruleValue(new BigDecimal("15000.00"))
                    .ruleMetadata("{\"admin_min_amount\": 500.00}")
                    .effectiveFrom(LocalDate.of(2024, 4, 1))
                    .effectiveTo(null)
                    .description("EPF wage ceiling")
                    .build();

            when(statutoryRuleRepository.findEffectiveRule("EPF_WAGE_CEILING", date2024))
                    .thenReturn(Optional.of(rule));

            Optional<StatutoryRule> result = statutoryQueryService.getEffectiveRule("EPF_WAGE_CEILING", date2024);
            assertTrue(result.isPresent());

            StatutoryRuleDto dto = statutoryQueryService.toDto(result.get());
            assertNotNull(dto);
            assertEquals(ruleId, dto.getId());
            assertEquals("EPF_WAGE_CEILING", dto.getRuleKey());
            assertEquals(new BigDecimal("15000.00"), dto.getRuleValue());
            assertEquals("{\"admin_min_amount\": 500.00}", dto.getRuleMetadata());
            assertEquals(LocalDate.of(2024, 4, 1), dto.getEffectiveFrom());
            assertNull(dto.getEffectiveTo());
            assertEquals("EPF wage ceiling", dto.getDescription());
        }
    }

    @Nested
    @DisplayName("ESIC Wage Ceiling Tests")
    class EsicWageCeilingTests {

        @Test
        @DisplayName("getEsicWageCeiling should return INR 21,000 threshold")
        void shouldReturnEsicWageCeiling() {
            StatutoryRule esicRule = StatutoryRule.builder()
                    .id(UUID.randomUUID())
                    .ruleKey("ESIC_WAGE_CEILING")
                    .ruleValue(new BigDecimal("21000.00"))
                    .ruleMetadata("{\"employee_rate\": 0.0075, \"employer_rate\": 0.0325, \"pwd_ceiling\": 25000.00}")
                    .effectiveFrom(LocalDate.of(2024, 4, 1))
                    .effectiveTo(null)
                    .description("ESIC gross monthly wage threshold of INR 21,000")
                    .build();

            when(statutoryRuleRepository.findEffectiveRule("ESIC_WAGE_CEILING", date2026))
                    .thenReturn(Optional.of(esicRule));

            BigDecimal ceiling = statutoryQueryService.getEsicWageCeiling(date2026);

            assertEquals(new BigDecimal("21000.00"), ceiling);
            verify(statutoryRuleRepository).findEffectiveRule("ESIC_WAGE_CEILING", date2026);
        }

        @Test
        @DisplayName("getEsicWageCeiling should throw IllegalStateException when rule is missing")
        void shouldThrowWhenEsicRuleMissing() {
            when(statutoryRuleRepository.findEffectiveRule("ESIC_WAGE_CEILING", date2026))
                    .thenReturn(Optional.empty());

            assertThrows(IllegalStateException.class,
                    () -> statutoryQueryService.getEsicWageCeiling(date2026));
        }
    }

    @Nested
    @DisplayName("Basic Wage Ratio Tests")
    class BasicWageRatioTests {

        @Test
        @DisplayName("getBasicWageRatioMin should return 0.50 (50% under Code on Wages)")
        void shouldReturnBasicWageRatioMin() {
            StatutoryRule wageRatioRule = StatutoryRule.builder()
                    .id(UUID.randomUUID())
                    .ruleKey("BASIC_WAGE_RATIO_MIN")
                    .ruleValue(new BigDecimal("0.50"))
                    .ruleMetadata("{\"statute\": \"Code on Wages 2019\", \"reallocation_target\": \"STATUTORY_WAGE_BASE\"}")
                    .effectiveFrom(LocalDate.of(2024, 4, 1))
                    .effectiveTo(null)
                    .description("Code on Wages 50% basic ratio")
                    .build();

            when(statutoryRuleRepository.findEffectiveRule("BASIC_WAGE_RATIO_MIN", date2026))
                    .thenReturn(Optional.of(wageRatioRule));

            BigDecimal ratio = statutoryQueryService.getBasicWageRatioMin(date2026);

            assertEquals(new BigDecimal("0.50"), ratio);
            verify(statutoryRuleRepository).findEffectiveRule("BASIC_WAGE_RATIO_MIN", date2026);
        }

        @Test
        @DisplayName("getBasicWageRatioMin should throw IllegalStateException when rule missing")
        void shouldThrowWhenRatioRuleMissing() {
            when(statutoryRuleRepository.findEffectiveRule("BASIC_WAGE_RATIO_MIN", date2026))
                    .thenReturn(Optional.empty());

            assertThrows(IllegalStateException.class,
                    () -> statutoryQueryService.getBasicWageRatioMin(date2026));
        }
    }

    @Nested
    @DisplayName("Karnataka Professional Tax (PT) Slab Tests")
    class KarnatakaPtTests {

        @Test
        @DisplayName("getPtSlab for KA on INR 25,000 salary should yield INR 200 PT")
        void shouldReturn200ForKarnatakaOn25kSalary() {
            PtSlab kaSlab200 = PtSlab.builder()
                    .id(UUID.randomUUID())
                    .stateCode("KA")
                    .gender("ALL")
                    .salaryFrom(new BigDecimal("15000.00"))
                    .salaryTo(null)
                    .taxAmount(new BigDecimal("200.00"))
                    .effectiveFrom(LocalDate.of(2024, 4, 1))
                    .effectiveTo(null)
                    .build();

            BigDecimal salary = new BigDecimal("25000.00");
            when(ptSlabRepository.findApplicableSlab("KA", "ALL", salary, date2026))
                    .thenReturn(Optional.of(kaSlab200));

            Optional<PtSlab> slab = statutoryQueryService.getPtSlab("KA", null, salary, date2026);

            assertTrue(slab.isPresent());
            assertEquals("KA", slab.get().getStateCode());
            assertEquals(new BigDecimal("200.00"), slab.get().getTaxAmount());
            assertNull(slab.get().getSpecialMonth());
        }

        @Test
        @DisplayName("getPtSlab for KA on INR 10,000 salary should yield INR 0 PT")
        void shouldReturn0ForKarnatakaBelowThreshold() {
            PtSlab kaSlab0 = PtSlab.builder()
                    .id(UUID.randomUUID())
                    .stateCode("KA")
                    .gender("ALL")
                    .salaryFrom(BigDecimal.ZERO)
                    .salaryTo(new BigDecimal("14999.99"))
                    .taxAmount(BigDecimal.ZERO)
                    .effectiveFrom(LocalDate.of(2024, 4, 1))
                    .effectiveTo(null)
                    .build();

            BigDecimal salary = new BigDecimal("10000.00");
            when(ptSlabRepository.findApplicableSlab("KA", "ALL", salary, date2026))
                    .thenReturn(Optional.of(kaSlab0));

            Optional<PtSlab> slab = statutoryQueryService.getPtSlab("ka", "", salary, date2026);

            assertTrue(slab.isPresent());
            assertEquals(BigDecimal.ZERO, slab.get().getTaxAmount());
            verify(ptSlabRepository).findApplicableSlab("KA", "ALL", salary, date2026);
        }

        @Test
        @DisplayName("getPtSlabDto for KA should return populated PtSlabDto")
        void shouldReturnPtSlabDtoForKarnataka() {
            UUID slabId = UUID.randomUUID();
            PtSlab kaSlab200 = PtSlab.builder()
                    .id(slabId)
                    .stateCode("KA")
                    .gender("ALL")
                    .salaryFrom(new BigDecimal("15000.00"))
                    .salaryTo(null)
                    .taxAmount(new BigDecimal("200.00"))
                    .effectiveFrom(LocalDate.of(2024, 4, 1))
                    .effectiveTo(null)
                    .build();

            BigDecimal salary = new BigDecimal("25000.00");
            when(ptSlabRepository.findApplicableSlab("KA", "ALL", salary, date2026))
                    .thenReturn(Optional.of(kaSlab200));

            Optional<PtSlabDto> dto = statutoryQueryService.getPtSlabDto("KA", "ALL", salary, date2026);

            assertTrue(dto.isPresent());
            assertEquals(slabId, dto.get().getId());
            assertEquals("KA", dto.get().getStateCode());
            assertEquals("ALL", dto.get().getGender());
            assertEquals(new BigDecimal("15000.00"), dto.get().getSalaryFrom());
            assertNull(dto.get().getSalaryTo());
            assertEquals(new BigDecimal("200.00"), dto.get().getTaxAmount());
        }

        @Test
        @DisplayName("getPtSlabsByState for KA should return all configured slabs")
        void shouldReturnAllPtSlabsForKarnataka() {
            PtSlab slab1 = PtSlab.builder()
                    .stateCode("KA")
                    .gender("ALL")
                    .salaryFrom(BigDecimal.ZERO)
                    .salaryTo(new BigDecimal("14999.99"))
                    .taxAmount(BigDecimal.ZERO)
                    .effectiveFrom(LocalDate.of(2024, 4, 1))
                    .build();

            PtSlab slab2 = PtSlab.builder()
                    .stateCode("KA")
                    .gender("ALL")
                    .salaryFrom(new BigDecimal("15000.00"))
                    .salaryTo(null)
                    .taxAmount(new BigDecimal("200.00"))
                    .effectiveFrom(LocalDate.of(2024, 4, 1))
                    .build();

            when(ptSlabRepository.findSlabsByState("KA", date2026))
                    .thenReturn(List.of(slab1, slab2));

            List<PtSlabDto> slabs = statutoryQueryService.getPtSlabDtosByState("ka", date2026);

            assertEquals(2, slabs.size());
            assertEquals(BigDecimal.ZERO, slabs.get(0).getTaxAmount());
            assertEquals(new BigDecimal("200.00"), slabs.get(1).getTaxAmount());
        }
    }

    @Nested
    @DisplayName("Maharashtra Professional Tax (PT) Slab Tests")
    class MaharashtraPtTests {

        @Test
        @DisplayName("MH Male salary <= 7,500 should yield INR 0 PT")
        void shouldReturn0ForMhMaleUnder7500() {
            PtSlab slab = PtSlab.builder()
                    .stateCode("MH")
                    .gender("MALE")
                    .salaryFrom(BigDecimal.ZERO)
                    .salaryTo(new BigDecimal("7500.00"))
                    .taxAmount(BigDecimal.ZERO)
                    .effectiveFrom(LocalDate.of(2024, 4, 1))
                    .build();

            BigDecimal salary = new BigDecimal("6000.00");
            when(ptSlabRepository.findApplicableSlab("MH", "MALE", salary, date2026))
                    .thenReturn(Optional.of(slab));

            Optional<PtSlab> result = statutoryQueryService.getPtSlab("MH", "MALE", salary, date2026);

            assertTrue(result.isPresent());
            assertEquals(BigDecimal.ZERO, result.get().getTaxAmount());
        }

        @Test
        @DisplayName("MH Male salary 7,500.01 - 10,000 should yield INR 175 PT")
        void shouldReturn175ForMhMaleMidBracket() {
            PtSlab slab = PtSlab.builder()
                    .stateCode("MH")
                    .gender("MALE")
                    .salaryFrom(new BigDecimal("7500.01"))
                    .salaryTo(new BigDecimal("10000.00"))
                    .taxAmount(new BigDecimal("175.00"))
                    .effectiveFrom(LocalDate.of(2024, 4, 1))
                    .build();

            BigDecimal salary = new BigDecimal("8500.00");
            when(ptSlabRepository.findApplicableSlab("MH", "MALE", salary, date2026))
                    .thenReturn(Optional.of(slab));

            Optional<PtSlab> result = statutoryQueryService.getPtSlab("mh", "male", salary, date2026);

            assertTrue(result.isPresent());
            assertEquals(new BigDecimal("175.00"), result.get().getTaxAmount());
        }

        @Test
        @DisplayName("MH Male salary > 10,000 should yield INR 200 PT (with Feb special INR 300)")
        void shouldReturn200ForMhMaleUpperBracketWithFebSpecial() {
            PtSlab slab = PtSlab.builder()
                    .stateCode("MH")
                    .gender("MALE")
                    .salaryFrom(new BigDecimal("10000.01"))
                    .salaryTo(null)
                    .taxAmount(new BigDecimal("200.00"))
                    .specialMonth(2)
                    .specialMonthTaxAmount(new BigDecimal("300.00"))
                    .effectiveFrom(LocalDate.of(2024, 4, 1))
                    .build();

            BigDecimal salary = new BigDecimal("15000.00");
            when(ptSlabRepository.findApplicableSlab("MH", "MALE", salary, date2026))
                    .thenReturn(Optional.of(slab));

            Optional<PtSlab> result = statutoryQueryService.getPtSlab("MH", "MALE", salary, date2026);

            assertTrue(result.isPresent());
            assertEquals(new BigDecimal("200.00"), result.get().getTaxAmount());
            assertEquals(2, result.get().getSpecialMonth());
            assertEquals(new BigDecimal("300.00"), result.get().getSpecialMonthTaxAmount());
        }

        @Test
        @DisplayName("MH Female salary <= 25,000 should yield INR 0 PT (exempt)")
        void shouldReturn0ForMhFemaleUnder25kExemption() {
            PtSlab slab = PtSlab.builder()
                    .stateCode("MH")
                    .gender("FEMALE")
                    .salaryFrom(BigDecimal.ZERO)
                    .salaryTo(new BigDecimal("25000.00"))
                    .taxAmount(BigDecimal.ZERO)
                    .effectiveFrom(LocalDate.of(2024, 4, 1))
                    .build();

            BigDecimal salary = new BigDecimal("20000.00");
            when(ptSlabRepository.findApplicableSlab("MH", "FEMALE", salary, date2026))
                    .thenReturn(Optional.of(slab));

            Optional<PtSlab> result = statutoryQueryService.getPtSlab("MH", "FEMALE", salary, date2026);

            assertTrue(result.isPresent());
            assertEquals(BigDecimal.ZERO, result.get().getTaxAmount());
        }

        @Test
        @DisplayName("MH Female salary > 25,000 should yield INR 200 PT (with Feb special INR 300)")
        void shouldReturn200ForMhFemaleAbove25k() {
            PtSlab slab = PtSlab.builder()
                    .stateCode("MH")
                    .gender("FEMALE")
                    .salaryFrom(new BigDecimal("25000.01"))
                    .salaryTo(null)
                    .taxAmount(new BigDecimal("200.00"))
                    .specialMonth(2)
                    .specialMonthTaxAmount(new BigDecimal("300.00"))
                    .effectiveFrom(LocalDate.of(2024, 4, 1))
                    .build();

            BigDecimal salary = new BigDecimal("35000.00");
            when(ptSlabRepository.findApplicableSlab("MH", "FEMALE", salary, date2026))
                    .thenReturn(Optional.of(slab));

            Optional<PtSlab> result = statutoryQueryService.getPtSlab("mh", "female", salary, date2026);

            assertTrue(result.isPresent());
            assertEquals(new BigDecimal("200.00"), result.get().getTaxAmount());
            assertEquals(2, result.get().getSpecialMonth());
            assertEquals(new BigDecimal("300.00"), result.get().getSpecialMonthTaxAmount());
        }
    }

    @Nested
    @DisplayName("TDS Slab Retrieval Tests (FY 2026-2027)")
    class TdsTaxSlabTests {

        @Test
        @DisplayName("getTdsSlabs for NEW regime should return 6 progressive slabs with INR 75k standard deduction")
        void shouldReturnNewRegimeTdsSlabs() {
            List<TdsTaxSlab> newSlabs = List.of(
                    TdsTaxSlab.builder().financialYear("2026-2027").regime("NEW").incomeFrom(new BigDecimal("0.00")).incomeTo(new BigDecimal("300000.00")).taxRate(new BigDecimal("0.00")).standardDeduction(new BigDecimal("75000.00")).rebate87aLimit(new BigDecimal("1275000.00")).build(),
                    TdsTaxSlab.builder().financialYear("2026-2027").regime("NEW").incomeFrom(new BigDecimal("300000.01")).incomeTo(new BigDecimal("700000.00")).taxRate(new BigDecimal("5.00")).standardDeduction(new BigDecimal("75000.00")).rebate87aLimit(new BigDecimal("1275000.00")).build(),
                    TdsTaxSlab.builder().financialYear("2026-2027").regime("NEW").incomeFrom(new BigDecimal("700000.01")).incomeTo(new BigDecimal("1000000.00")).taxRate(new BigDecimal("10.00")).standardDeduction(new BigDecimal("75000.00")).rebate87aLimit(new BigDecimal("1275000.00")).build(),
                    TdsTaxSlab.builder().financialYear("2026-2027").regime("NEW").incomeFrom(new BigDecimal("1000000.01")).incomeTo(new BigDecimal("1200000.00")).taxRate(new BigDecimal("15.00")).standardDeduction(new BigDecimal("75000.00")).rebate87aLimit(new BigDecimal("1275000.00")).build(),
                    TdsTaxSlab.builder().financialYear("2026-2027").regime("NEW").incomeFrom(new BigDecimal("1200000.01")).incomeTo(new BigDecimal("1500000.00")).taxRate(new BigDecimal("20.00")).standardDeduction(new BigDecimal("75000.00")).rebate87aLimit(new BigDecimal("1275000.00")).build(),
                    TdsTaxSlab.builder().financialYear("2026-2027").regime("NEW").incomeFrom(new BigDecimal("1500000.01")).incomeTo(null).taxRate(new BigDecimal("30.00")).standardDeduction(new BigDecimal("75000.00")).rebate87aLimit(new BigDecimal("1275000.00")).build()
            );

            when(tdsTaxSlabRepository.findByFinancialYearAndRegimeOrderByIncomeFromAsc("2026-2027", "NEW"))
                    .thenReturn(newSlabs);

            List<TdsTaxSlabDto> dtos = statutoryQueryService.getTdsSlabDtos("2026-2027", "new");

            assertEquals(6, dtos.size());
            assertEquals(new BigDecimal("0.00"), dtos.get(0).getTaxRate());
            assertEquals(new BigDecimal("5.00"), dtos.get(1).getTaxRate());
            assertEquals(new BigDecimal("10.00"), dtos.get(2).getTaxRate());
            assertEquals(new BigDecimal("15.00"), dtos.get(3).getTaxRate());
            assertEquals(new BigDecimal("20.00"), dtos.get(4).getTaxRate());
            assertEquals(new BigDecimal("30.00"), dtos.get(5).getTaxRate());

            for (TdsTaxSlabDto dto : dtos) {
                assertEquals("2026-2027", dto.getFinancialYear());
                assertEquals("NEW", dto.getRegime());
                assertEquals(new BigDecimal("75000.00"), dto.getStandardDeduction());
                assertEquals(new BigDecimal("1275000.00"), dto.getRebate87aLimit());
            }
        }

        @Test
        @DisplayName("getTdsSlabs for OLD regime should return 4 slabs with INR 50k standard deduction")
        void shouldReturnOldRegimeTdsSlabs() {
            List<TdsTaxSlab> oldSlabs = List.of(
                    TdsTaxSlab.builder().financialYear("2026-2027").regime("OLD").incomeFrom(new BigDecimal("0.00")).incomeTo(new BigDecimal("250000.00")).taxRate(new BigDecimal("0.00")).standardDeduction(new BigDecimal("50000.00")).rebate87aLimit(new BigDecimal("500000.00")).build(),
                    TdsTaxSlab.builder().financialYear("2026-2027").regime("OLD").incomeFrom(new BigDecimal("250000.01")).incomeTo(new BigDecimal("500000.00")).taxRate(new BigDecimal("5.00")).standardDeduction(new BigDecimal("50000.00")).rebate87aLimit(new BigDecimal("500000.00")).build(),
                    TdsTaxSlab.builder().financialYear("2026-2027").regime("OLD").incomeFrom(new BigDecimal("500000.01")).incomeTo(new BigDecimal("1000000.00")).taxRate(new BigDecimal("20.00")).standardDeduction(new BigDecimal("50000.00")).rebate87aLimit(new BigDecimal("500000.00")).build(),
                    TdsTaxSlab.builder().financialYear("2026-2027").regime("OLD").incomeFrom(new BigDecimal("1000000.01")).incomeTo(null).taxRate(new BigDecimal("30.00")).standardDeduction(new BigDecimal("50000.00")).rebate87aLimit(new BigDecimal("500000.00")).build()
            );

            when(tdsTaxSlabRepository.findByFinancialYearAndRegimeOrderByIncomeFromAsc("2026-2027", "OLD"))
                    .thenReturn(oldSlabs);

            List<TdsTaxSlabDto> dtos = statutoryQueryService.getTdsSlabDtos("2026-2027", "OLD");

            assertEquals(4, dtos.size());
            assertEquals(new BigDecimal("0.00"), dtos.get(0).getTaxRate());
            assertEquals(new BigDecimal("5.00"), dtos.get(1).getTaxRate());
            assertEquals(new BigDecimal("20.00"), dtos.get(2).getTaxRate());
            assertEquals(new BigDecimal("30.00"), dtos.get(3).getTaxRate());

            for (TdsTaxSlabDto dto : dtos) {
                assertEquals("2026-2027", dto.getFinancialYear());
                assertEquals("OLD", dto.getRegime());
                assertEquals(new BigDecimal("50000.00"), dto.getStandardDeduction());
                assertEquals(new BigDecimal("500000.00"), dto.getRebate87aLimit());
            }
        }
    }

    @Nested
    @DisplayName("Labour Welfare Fund (LWF) Rule Tests")
    class LwfRuleTests {

        @Test
        @DisplayName("getLwfRule for KA should return annual December deduction")
        void shouldReturnKarnatakaLwfRule() {
            LwfRule kaLwf = LwfRule.builder()
                    .id(UUID.randomUUID())
                    .stateCode("KA")
                    .employeeContribution(new BigDecimal("20.00"))
                    .employerContribution(new BigDecimal("40.00"))
                    .deductionFrequency("ANNUAL")
                    .deductionMonths(List.of(12))
                    .effectiveFrom(LocalDate.of(2024, 4, 1))
                    .build();

            when(lwfRuleRepository.findEffectiveRule("KA", date2026))
                    .thenReturn(Optional.of(kaLwf));

            Optional<LwfRuleDto> dto = statutoryQueryService.getLwfRuleDto("ka", date2026);

            assertTrue(dto.isPresent());
            assertEquals("KA", dto.get().getStateCode());
            assertEquals(new BigDecimal("20.00"), dto.get().getEmployeeContribution());
            assertEquals(new BigDecimal("40.00"), dto.get().getEmployerContribution());
            assertEquals("ANNUAL", dto.get().getDeductionFrequency());
            assertEquals(List.of(12), dto.get().getDeductionMonths());
        }

        @Test
        @DisplayName("getLwfRule for MH should return half-yearly June/December deduction")
        void shouldReturnMaharashtraLwfRule() {
            LwfRule mhLwf = LwfRule.builder()
                    .id(UUID.randomUUID())
                    .stateCode("MH")
                    .employeeContribution(new BigDecimal("12.00"))
                    .employerContribution(new BigDecimal("36.00"))
                    .deductionFrequency("HALF_YEARLY")
                    .deductionMonths(List.of(6, 12))
                    .effectiveFrom(LocalDate.of(2024, 4, 1))
                    .build();

            when(lwfRuleRepository.findEffectiveRule("MH", date2026))
                    .thenReturn(Optional.of(mhLwf));

            Optional<LwfRuleDto> dto = statutoryQueryService.getLwfRuleDto("MH", date2026);

            assertTrue(dto.isPresent());
            assertEquals("MH", dto.get().getStateCode());
            assertEquals(new BigDecimal("12.00"), dto.get().getEmployeeContribution());
            assertEquals(new BigDecimal("36.00"), dto.get().getEmployerContribution());
            assertEquals("HALF_YEARLY", dto.get().getDeductionFrequency());
            assertEquals(List.of(6, 12), dto.get().getDeductionMonths());
        }
    }

    @Nested
    @DisplayName("Defensive & Boundary Handling Tests")
    class DefensiveAndBoundaryTests {

        @Test
        @DisplayName("getPtSlab with null stateCode should return empty Optional")
        void shouldReturnEmptyWhenStateCodeIsNull() {
            Optional<PtSlab> slab = statutoryQueryService.getPtSlab(null, "ALL", new BigDecimal("25000.00"), date2026);
            assertFalse(slab.isPresent());
        }

        @Test
        @DisplayName("getPtSlab with null salary should return empty Optional")
        void shouldReturnEmptyWhenSalaryIsNull() {
            Optional<PtSlab> slab = statutoryQueryService.getPtSlab("KA", "ALL", null, date2026);
            assertFalse(slab.isPresent());
        }

        @Test
        @DisplayName("getPtSlabsByState with null stateCode should return empty List")
        void shouldReturnEmptyListWhenPtStateCodeIsNull() {
            List<PtSlab> slabs = statutoryQueryService.getPtSlabsByState(null, date2026);
            assertTrue(slabs.isEmpty());
        }

        @Test
        @DisplayName("getLwfRule with null stateCode should return empty Optional")
        void shouldReturnEmptyWhenLwfStateCodeIsNull() {
            Optional<LwfRule> rule = statutoryQueryService.getLwfRule(null, date2026);
            assertFalse(rule.isPresent());
        }

        @Test
        @DisplayName("getTdsSlabs with null financialYear or regime should return empty List")
        void shouldReturnEmptyListWhenTdsParamsAreNull() {
            assertTrue(statutoryQueryService.getTdsSlabs(null, "NEW").isEmpty());
            assertTrue(statutoryQueryService.getTdsSlabs("2026-2027", null).isEmpty());
        }

        @Test
        @DisplayName("toDto converters should safely return null for null entities")
        void shouldReturnNullWhenEntitiesAreNull() {
            assertNull(statutoryQueryService.toDto((StatutoryRule) null));
            assertNull(statutoryQueryService.toDto((PtSlab) null));
            assertNull(statutoryQueryService.toDto((LwfRule) null));
            assertNull(statutoryQueryService.toDto((TdsTaxSlab) null));
        }

        @Test
        @DisplayName("null onDate should default to current date")
        void shouldDefaultToCurrentDateWhenOnDateIsNull() {
            StatutoryRule rule = StatutoryRule.builder()
                    .ruleKey("EPF_WAGE_CEILING")
                    .ruleValue(new BigDecimal("15000.00"))
                    .effectiveFrom(LocalDate.of(2024, 4, 1))
                    .build();

            when(statutoryRuleRepository.findEffectiveRule(eq("EPF_WAGE_CEILING"), any(LocalDate.class)))
                    .thenReturn(Optional.of(rule));

            BigDecimal ceiling = statutoryQueryService.getEpfWageCeiling(null);

            assertEquals(new BigDecimal("15000.00"), ceiling);
            verify(statutoryRuleRepository).findEffectiveRule(eq("EPF_WAGE_CEILING"), eq(LocalDate.now()));
        }
    }
}

package com.ourhr.statutory;

import com.ourhr.statutory.dto.LwfRuleDto;
import com.ourhr.statutory.dto.PtSlabDto;
import com.ourhr.statutory.dto.StatutoryRuleDto;
import com.ourhr.statutory.dto.TdsTaxSlabDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StatutoryQueryService {

    public static final String RULE_KEY_EPF_WAGE_CEILING = "EPF_WAGE_CEILING";
    public static final String RULE_KEY_ESIC_WAGE_CEILING = "ESIC_WAGE_CEILING";
    public static final String RULE_KEY_BASIC_WAGE_RATIO_MIN = "BASIC_WAGE_RATIO_MIN";

    private final StatutoryRuleRepository statutoryRuleRepository;
    private final PtSlabRepository ptSlabRepository;
    private final LwfRuleRepository lwfRuleRepository;
    private final TdsTaxSlabRepository tdsTaxSlabRepository;

    public Optional<StatutoryRule> getEffectiveRule(String ruleKey, LocalDate onDate) {
        LocalDate queryDate = resolveDate(onDate);
        return statutoryRuleRepository.findEffectiveRule(ruleKey, queryDate);
    }

    public BigDecimal getRuleValue(String ruleKey, LocalDate onDate) {
        LocalDate queryDate = resolveDate(onDate);
        return statutoryRuleRepository.findEffectiveRule(ruleKey, queryDate)
                .map(StatutoryRule::getRuleValue)
                .orElseThrow(() -> new IllegalStateException(
                        "Statutory rule not configured for key: " + ruleKey + " on date: " + queryDate));
    }

    public BigDecimal getEpfWageCeiling(LocalDate onDate) {
        return getRuleValue(RULE_KEY_EPF_WAGE_CEILING, onDate);
    }

    public BigDecimal getEsicWageCeiling(LocalDate onDate) {
        return getRuleValue(RULE_KEY_ESIC_WAGE_CEILING, onDate);
    }

    public BigDecimal getBasicWageRatioMin(LocalDate onDate) {
        return getRuleValue(RULE_KEY_BASIC_WAGE_RATIO_MIN, onDate);
    }

    public Optional<PtSlab> getPtSlab(String stateCode, String gender, BigDecimal monthlySalary, LocalDate onDate) {
        if (stateCode == null || monthlySalary == null) {
            return Optional.empty();
        }
        String normalizedState = stateCode.trim().toUpperCase();
        String normalizedGender = (gender != null && !gender.isBlank()) ? gender.trim().toUpperCase() : "ALL";
        LocalDate queryDate = resolveDate(onDate);
        return ptSlabRepository.findApplicableSlab(normalizedState, normalizedGender, monthlySalary, queryDate);
    }

    public List<PtSlab> getPtSlabsByState(String stateCode, LocalDate onDate) {
        if (stateCode == null) {
            return List.of();
        }
        String normalizedState = stateCode.trim().toUpperCase();
        LocalDate queryDate = resolveDate(onDate);
        return ptSlabRepository.findSlabsByState(normalizedState, queryDate);
    }

    public Optional<LwfRule> getLwfRule(String stateCode, LocalDate onDate) {
        if (stateCode == null) {
            return Optional.empty();
        }
        String normalizedState = stateCode.trim().toUpperCase();
        LocalDate queryDate = resolveDate(onDate);
        return lwfRuleRepository.findEffectiveRule(normalizedState, queryDate);
    }

    public List<TdsTaxSlab> getTdsSlabs(String financialYear, String regime) {
        if (financialYear == null || regime == null) {
            return List.of();
        }
        String normalizedRegime = regime.trim().toUpperCase();
        return tdsTaxSlabRepository.findByFinancialYearAndRegimeOrderByIncomeFromAsc(financialYear.trim(), normalizedRegime);
    }

    public Optional<PtSlabDto> getPtSlabDto(String stateCode, String gender, BigDecimal monthlySalary, LocalDate onDate) {
        return getPtSlab(stateCode, gender, monthlySalary, onDate).map(this::toDto);
    }

    public List<PtSlabDto> getPtSlabDtosByState(String stateCode, LocalDate onDate) {
        return getPtSlabsByState(stateCode, onDate).stream()
                .map(this::toDto)
                .toList();
    }

    public Optional<LwfRuleDto> getLwfRuleDto(String stateCode, LocalDate onDate) {
        return getLwfRule(stateCode, onDate).map(this::toDto);
    }

    public List<TdsTaxSlabDto> getTdsSlabDtos(String financialYear, String regime) {
        return getTdsSlabs(financialYear, regime).stream()
                .map(this::toDto)
                .toList();
    }

    public StatutoryRuleDto toDto(StatutoryRule rule) {
        if (rule == null) {
            return null;
        }
        return StatutoryRuleDto.builder()
                .id(rule.getId())
                .ruleKey(rule.getRuleKey())
                .ruleValue(rule.getRuleValue())
                .ruleMetadata(rule.getRuleMetadata())
                .effectiveFrom(rule.getEffectiveFrom())
                .effectiveTo(rule.getEffectiveTo())
                .description(rule.getDescription())
                .build();
    }

    public PtSlabDto toDto(PtSlab slab) {
        if (slab == null) {
            return null;
        }
        return PtSlabDto.builder()
                .id(slab.getId())
                .stateCode(slab.getStateCode())
                .gender(slab.getGender())
                .salaryFrom(slab.getSalaryFrom())
                .salaryTo(slab.getSalaryTo())
                .taxAmount(slab.getTaxAmount())
                .specialMonth(slab.getSpecialMonth())
                .specialMonthTaxAmount(slab.getSpecialMonthTaxAmount())
                .effectiveFrom(slab.getEffectiveFrom())
                .effectiveTo(slab.getEffectiveTo())
                .build();
    }

    public LwfRuleDto toDto(LwfRule rule) {
        if (rule == null) {
            return null;
        }
        return LwfRuleDto.builder()
                .id(rule.getId())
                .stateCode(rule.getStateCode())
                .employeeContribution(rule.getEmployeeContribution())
                .employerContribution(rule.getEmployerContribution())
                .deductionFrequency(rule.getDeductionFrequency())
                .deductionMonths(rule.getDeductionMonths())
                .effectiveFrom(rule.getEffectiveFrom())
                .effectiveTo(rule.getEffectiveTo())
                .build();
    }

    public TdsTaxSlabDto toDto(TdsTaxSlab slab) {
        if (slab == null) {
            return null;
        }
        return TdsTaxSlabDto.builder()
                .id(slab.getId())
                .financialYear(slab.getFinancialYear())
                .regime(slab.getRegime())
                .incomeFrom(slab.getIncomeFrom())
                .incomeTo(slab.getIncomeTo())
                .taxRate(slab.getTaxRate())
                .standardDeduction(slab.getStandardDeduction())
                .rebate87aLimit(slab.getRebate87aLimit())
                .build();
    }

    private LocalDate resolveDate(LocalDate onDate) {
        return onDate != null ? onDate : LocalDate.now();
    }
}

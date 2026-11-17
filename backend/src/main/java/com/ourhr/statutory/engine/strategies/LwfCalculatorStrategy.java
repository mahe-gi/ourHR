package com.ourhr.statutory.engine.strategies;

import com.ourhr.statutory.LwfRule;
import com.ourhr.statutory.StatutoryQueryService;
import com.ourhr.statutory.engine.StatutoryCalculationStrategy;
import com.ourhr.statutory.engine.dto.LwfCalculationResult;
import com.ourhr.statutory.engine.dto.StatutoryCalculationRequest;
import com.ourhr.statutory.engine.dto.StatutoryCalculationResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class LwfCalculatorStrategy implements StatutoryCalculationStrategy {

    public static final String STATUTORY_TYPE = "LWF";
    private static final String DEFAULT_STATE = "KA";

    private final StatutoryQueryService statutoryQueryService;

    @Override
    public boolean supports(String statutoryType) {
        return STATUTORY_TYPE.equalsIgnoreCase(statutoryType);
    }

    @Override
    public void calculate(StatutoryCalculationRequest request,
                          StatutoryCalculationResult.StatutoryCalculationResultBuilder resultBuilder) {
        LocalDate queryDate = resolveQueryDate(request);
        String stateCode = (request.getBranchStateCode() != null && !request.getBranchStateCode().isBlank())
                ? request.getBranchStateCode().trim().toUpperCase()
                : DEFAULT_STATE;

        Optional<LwfRule> ruleOpt = statutoryQueryService.getLwfRule(stateCode, queryDate);

        boolean applicable = false;
        BigDecimal employeeContribution = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        BigDecimal employerContribution = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalContribution = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

        if (ruleOpt.isPresent()) {
            LwfRule rule = ruleOpt.get();
            List<Integer> deductionMonths = rule.getDeductionMonths();
            if (deductionMonths != null && deductionMonths.contains(request.getPayrollMonth())) {
                applicable = true;
                employeeContribution = rule.getEmployeeContribution() != null
                        ? rule.getEmployeeContribution().setScale(2, RoundingMode.HALF_UP)
                        : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
                employerContribution = rule.getEmployerContribution() != null
                        ? rule.getEmployerContribution().setScale(2, RoundingMode.HALF_UP)
                        : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
                totalContribution = employeeContribution.add(employerContribution)
                        .setScale(2, RoundingMode.HALF_UP);
            }
        }

        LwfCalculationResult result = LwfCalculationResult.builder()
                .stateCode(stateCode)
                .applicable(applicable)
                .employeeContribution(employeeContribution)
                .employerContribution(employerContribution)
                .totalContribution(totalContribution)
                .build();

        resultBuilder.lwf(result);
    }

    private LocalDate resolveQueryDate(StatutoryCalculationRequest request) {
        if (request == null) {
            return LocalDate.now();
        }
        int year = request.getPayrollYear() > 0 ? request.getPayrollYear() : LocalDate.now().getYear();
        int month = (request.getPayrollMonth() >= 1 && request.getPayrollMonth() <= 12)
                ? request.getPayrollMonth()
                : LocalDate.now().getMonthValue();
        return LocalDate.of(year, month, 1);
    }
}

package com.ourhr.statutory.engine.strategies;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ourhr.statutory.StatutoryQueryService;
import com.ourhr.statutory.StatutoryRule;
import com.ourhr.statutory.engine.StatutoryCalculationStrategy;
import com.ourhr.statutory.engine.dto.EsicCalculationResult;
import com.ourhr.statutory.engine.dto.StatutoryCalculationRequest;
import com.ourhr.statutory.engine.dto.StatutoryCalculationResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class EsicCalculatorStrategy implements StatutoryCalculationStrategy {

    public static final String STATUTORY_TYPE = "ESIC";
    private static final BigDecimal DEFAULT_ESIC_CEILING = new BigDecimal("21000.00");
    private static final BigDecimal DEFAULT_PWD_CEILING = new BigDecimal("25000.00");
    private static final BigDecimal DEFAULT_EMPLOYEE_RATE = new BigDecimal("0.0075");
    private static final BigDecimal DEFAULT_EMPLOYER_RATE = new BigDecimal("0.0325");

    private final StatutoryQueryService statutoryQueryService;
    private final ObjectMapper objectMapper;

    @Override
    public boolean supports(String statutoryType) {
        return STATUTORY_TYPE.equalsIgnoreCase(statutoryType);
    }

    @Override
    public void calculate(StatutoryCalculationRequest request,
                          StatutoryCalculationResult.StatutoryCalculationResultBuilder resultBuilder) {
        LocalDate queryDate = resolveQueryDate(request);
        EsicCeilingAndRateConfig config = resolveCeilingAndRates(queryDate, request.isPwd());

        BigDecimal earnedGross = request.getEarnedGross() != null ? request.getEarnedGross() : BigDecimal.ZERO;
        BigDecimal initialGross = request.getInitialGrossInWindow();
        BigDecimal ceiling = config.ceiling;

        boolean eligible;
        boolean continuationRuleApplied = false;

        if (initialGross != null && initialGross.compareTo(ceiling) <= 0) {
            eligible = true;
            if (earnedGross.compareTo(ceiling) > 0) {
                continuationRuleApplied = true;
            }
        } else if (earnedGross.compareTo(ceiling) <= 0) {
            eligible = true;
        } else {
            eligible = false;
        }

        EsicCalculationResult result;
        if (eligible) {
            BigDecimal wageBase = earnedGross.setScale(2, RoundingMode.HALF_UP);
            BigDecimal employeeContribution = earnedGross.multiply(config.employeeRate)
                    .setScale(2, RoundingMode.HALF_UP);
            BigDecimal employerContribution = earnedGross.multiply(config.employerRate)
                    .setScale(2, RoundingMode.HALF_UP);
            BigDecimal totalLiability = employeeContribution.add(employerContribution)
                    .setScale(2, RoundingMode.HALF_UP);

            String note = continuationRuleApplied
                    ? "Covered under 6-month contribution window continuation rule"
                    : "Eligible for ESIC coverage";

            result = EsicCalculationResult.builder()
                    .eligible(true)
                    .wageBase(wageBase)
                    .employeeContribution(employeeContribution)
                    .employerContribution(employerContribution)
                    .totalLiability(totalLiability)
                    .note(note)
                    .build();
        } else {
            result = EsicCalculationResult.builder()
                    .eligible(false)
                    .wageBase(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP))
                    .employeeContribution(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP))
                    .employerContribution(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP))
                    .totalLiability(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP))
                    .note("Earned gross exceeds ESIC wage ceiling of " + ceiling)
                    .build();
        }

        resultBuilder.esic(result);
    }

    private EsicCeilingAndRateConfig resolveCeilingAndRates(LocalDate queryDate, boolean isPwd) {
        BigDecimal ceiling = isPwd ? DEFAULT_PWD_CEILING : DEFAULT_ESIC_CEILING;
        BigDecimal employeeRate = DEFAULT_EMPLOYEE_RATE;
        BigDecimal employerRate = DEFAULT_EMPLOYER_RATE;

        try {
            Optional<StatutoryRule> ruleOpt = statutoryQueryService.getEffectiveRule(
                    StatutoryQueryService.RULE_KEY_ESIC_WAGE_CEILING, queryDate);

            if (ruleOpt.isPresent()) {
                StatutoryRule rule = ruleOpt.get();
                if (!isPwd && rule.getRuleValue() != null) {
                    ceiling = rule.getRuleValue();
                }

                if (rule.getRuleMetadata() != null && !rule.getRuleMetadata().isBlank()) {
                    JsonNode root = objectMapper.readTree(rule.getRuleMetadata());
                    if (isPwd && root.has("pwd_ceiling")) {
                        ceiling = new BigDecimal(root.get("pwd_ceiling").asText());
                    }
                    if (root.has("employee_rate")) {
                        employeeRate = new BigDecimal(root.get("employee_rate").asText());
                    }
                    if (root.has("employer_rate")) {
                        employerRate = new BigDecimal(root.get("employer_rate").asText());
                    }
                }
            }
        } catch (Exception ignored) {
        }

        return new EsicCeilingAndRateConfig(ceiling, employeeRate, employerRate);
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

    private record EsicCeilingAndRateConfig(BigDecimal ceiling, BigDecimal employeeRate, BigDecimal employerRate) {
    }
}

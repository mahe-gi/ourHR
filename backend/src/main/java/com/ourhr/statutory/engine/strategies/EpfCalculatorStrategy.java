package com.ourhr.statutory.engine.strategies;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ourhr.statutory.StatutoryQueryService;
import com.ourhr.statutory.StatutoryRule;
import com.ourhr.statutory.engine.StatutoryCalculationStrategy;
import com.ourhr.statutory.engine.dto.EpfCalculationResult;
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
public class EpfCalculatorStrategy implements StatutoryCalculationStrategy {

    public static final String STATUTORY_TYPE = "EPF";
    private static final BigDecimal DEFAULT_EPF_CEILING = new BigDecimal("15000.00");
    private static final BigDecimal DEFAULT_EMPLOYEE_RATE = new BigDecimal("0.12");
    private static final BigDecimal DEFAULT_EPS_RATE = new BigDecimal("0.0833");
    private static final BigDecimal DEFAULT_EDLI_RATE = new BigDecimal("0.005");
    private static final BigDecimal DEFAULT_ADMIN_RATE = new BigDecimal("0.005");
    private static final BigDecimal EPS_MAX_CAP = new BigDecimal("1250.00");
    private static final BigDecimal EDLI_MAX_CAP = new BigDecimal("75.00");

    private final StatutoryQueryService statutoryQueryService;
    private final ObjectMapper objectMapper;

    @Override
    public boolean supports(String statutoryType) {
        return STATUTORY_TYPE.equalsIgnoreCase(statutoryType);
    }

    @Override
    public void calculate(StatutoryCalculationRequest request,
                          StatutoryCalculationResult.StatutoryCalculationResultBuilder resultBuilder) {
        if (request.isPfOptOut()) {
            EpfCalculationResult optOutResult = EpfCalculationResult.builder()
                    .wageBase(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP))
                    .employeeContribution(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP))
                    .employerEpsContribution(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP))
                    .employerEpfContribution(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP))
                    .edliContribution(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP))
                    .adminCharges(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP))
                    .totalEmployerLiability(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP))
                    .build();
            resultBuilder.epf(optOutResult);
            return;
        }

        LocalDate queryDate = resolveQueryDate(request);
        BigDecimal ceiling = resolveEpfCeiling(queryDate);
        EpfRateConfig rateConfig = resolveRateConfig(queryDate);

        BigDecimal basicPay = request.getBasicPay() != null ? request.getBasicPay() : BigDecimal.ZERO;
        BigDecimal daPay = request.getDaPay() != null ? request.getDaPay() : BigDecimal.ZERO;
        BigDecimal pfWages = basicPay.add(daPay).max(BigDecimal.ZERO);

        BigDecimal wageBase = pfWages.min(ceiling).setScale(2, RoundingMode.HALF_UP);

        BigDecimal employeeContribution = wageBase.multiply(rateConfig.employeeRate)
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal rawEps = (wageBase.compareTo(ceiling) >= 0)
                ? EPS_MAX_CAP
                : wageBase.multiply(rateConfig.epsRate).setScale(2, RoundingMode.HALF_UP);
        BigDecimal employerEpsContribution = rawEps.min(EPS_MAX_CAP).setScale(2, RoundingMode.HALF_UP);

        BigDecimal employerEpfContribution = wageBase.multiply(rateConfig.employeeRate)
                .subtract(employerEpsContribution)
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal rawEdli = wageBase.multiply(rateConfig.edliRate).setScale(2, RoundingMode.HALF_UP);
        BigDecimal edliContribution = rawEdli.min(EDLI_MAX_CAP).setScale(2, RoundingMode.HALF_UP);

        BigDecimal adminCharges = wageBase.multiply(rateConfig.adminRate).setScale(2, RoundingMode.HALF_UP);

        BigDecimal totalEmployerLiability = employerEpsContribution
                .add(employerEpfContribution)
                .add(edliContribution)
                .add(adminCharges)
                .setScale(2, RoundingMode.HALF_UP);

        EpfCalculationResult result = EpfCalculationResult.builder()
                .wageBase(wageBase)
                .employeeContribution(employeeContribution)
                .employerEpsContribution(employerEpsContribution)
                .employerEpfContribution(employerEpfContribution)
                .edliContribution(edliContribution)
                .adminCharges(adminCharges)
                .totalEmployerLiability(totalEmployerLiability)
                .build();

        resultBuilder.epf(result);
    }

    private BigDecimal resolveEpfCeiling(LocalDate queryDate) {
        try {
            BigDecimal ceiling = statutoryQueryService.getEpfWageCeiling(queryDate);
            return ceiling != null ? ceiling : DEFAULT_EPF_CEILING;
        } catch (Exception e) {
            return DEFAULT_EPF_CEILING;
        }
    }

    private EpfRateConfig resolveRateConfig(LocalDate queryDate) {
        BigDecimal empRate = DEFAULT_EMPLOYEE_RATE;
        BigDecimal epsRate = DEFAULT_EPS_RATE;
        BigDecimal edliRate = DEFAULT_EDLI_RATE;
        BigDecimal adminRate = DEFAULT_ADMIN_RATE;

        try {
            Optional<StatutoryRule> ruleOpt = statutoryQueryService.getEffectiveRule(
                    StatutoryQueryService.RULE_KEY_EPF_WAGE_CEILING, queryDate);
            if (ruleOpt.isPresent() && ruleOpt.get().getRuleMetadata() != null) {
                JsonNode root = objectMapper.readTree(ruleOpt.get().getRuleMetadata());
                if (root.has("employee_rate")) {
                    empRate = new BigDecimal(root.get("employee_rate").asText());
                }
                if (root.has("employer_eps_rate")) {
                    epsRate = new BigDecimal(root.get("employer_eps_rate").asText());
                }
                if (root.has("edli_rate")) {
                    edliRate = new BigDecimal(root.get("edli_rate").asText());
                }
                if (root.has("admin_rate")) {
                    adminRate = new BigDecimal(root.get("admin_rate").asText());
                }
            }
        } catch (Exception ignored) {
        }

        return new EpfRateConfig(empRate, epsRate, edliRate, adminRate);
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

    private record EpfRateConfig(BigDecimal employeeRate, BigDecimal epsRate, BigDecimal edliRate, BigDecimal adminRate) {
    }
}

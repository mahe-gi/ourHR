package com.ourhr.employee.salary;

import com.ourhr.employee.salary.dto.SalaryOfferRequest;
import com.ourhr.employee.salary.dto.SalaryOfferValidationResult;
import com.ourhr.statutory.StatutoryQueryService;
import com.ourhr.statutory.StatutoryRule;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

@Component
@RequiredArgsConstructor
public class BasicWageValidator {

    private static final BigDecimal DEFAULT_MIN_RATIO = new BigDecimal("0.50");

    private final StatutoryQueryService statutoryQueryService;

    public SalaryOfferValidationResult validate(SalaryOfferRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Salary offer request cannot be null");
        }

        BigDecimal monthlyGross = request.getMonthlyGross();
        if (monthlyGross == null || monthlyGross.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Monthly gross must be greater than zero");
        }

        LocalDate effectiveDate = request.getEffectiveFrom() != null ? request.getEffectiveFrom() : LocalDate.now();

        BigDecimal minRatio;
        try {
            minRatio = statutoryQueryService.getEffectiveRule(StatutoryQueryService.RULE_KEY_BASIC_WAGE_RATIO_MIN, effectiveDate)
                    .map(StatutoryRule::getRuleValue)
                    .orElse(DEFAULT_MIN_RATIO);
        } catch (Exception e) {
            minRatio = DEFAULT_MIN_RATIO;
        }

        BigDecimal basicPay = request.getBasicPay() != null ? request.getBasicPay() : BigDecimal.ZERO;
        BigDecimal daPay = request.getDaPay() != null ? request.getDaPay() : BigDecimal.ZERO;
        BigDecimal basicPlusDa = basicPay.add(daPay);

        BigDecimal wageRatio = basicPlusDa.divide(monthlyGross, 4, RoundingMode.HALF_UP);

        boolean valid = wageRatio.compareTo(minRatio) >= 0;
        BigDecimal statutoryDeficit;
        String message;

        if (!valid) {
            BigDecimal minRequiredAmount = monthlyGross.multiply(minRatio).setScale(2, RoundingMode.HALF_UP);
            statutoryDeficit = minRequiredAmount.subtract(basicPlusDa).setScale(2, RoundingMode.HALF_UP);
            if (statutoryDeficit.compareTo(BigDecimal.ZERO) < 0) {
                statutoryDeficit = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
            }
            String requiredPercentage = minRatio.multiply(new BigDecimal("100")).setScale(2, RoundingMode.HALF_UP) + "%";
            message = "Code on Wages 2019 violation: Basic Pay plus Dearness Allowance (" + basicPlusDa
                    + ") must constitute at least " + requiredPercentage + " of Monthly Gross Salary (" + monthlyGross + ").";
        } else {
            statutoryDeficit = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
            message = "Compliant with Code on Wages 2019";
        }

        return SalaryOfferValidationResult.builder()
                .valid(valid)
                .basicPlusDa(basicPlusDa)
                .monthlyGross(monthlyGross)
                .wageRatio(wageRatio)
                .minRequiredRatio(minRatio)
                .statutoryDeficit(statutoryDeficit)
                .message(message)
                .build();
    }

    public void validateOrThrow(SalaryOfferRequest request) {
        SalaryOfferValidationResult result = validate(request);
        if (!result.isValid()) {
            throw new IllegalArgumentException(result.getMessage());
        }
    }
}

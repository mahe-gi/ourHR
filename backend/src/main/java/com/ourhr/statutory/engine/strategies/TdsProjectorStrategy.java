package com.ourhr.statutory.engine.strategies;

import com.ourhr.statutory.StatutoryQueryService;
import com.ourhr.statutory.TdsTaxSlab;
import com.ourhr.statutory.engine.StatutoryCalculationStrategy;
import com.ourhr.statutory.engine.dto.StatutoryCalculationRequest;
import com.ourhr.statutory.engine.dto.StatutoryCalculationResult;
import com.ourhr.statutory.engine.dto.TdsCalculationResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

@Component
@RequiredArgsConstructor
public class TdsProjectorStrategy implements StatutoryCalculationStrategy {

    public static final String STATUTORY_TYPE = "TDS";
    private static final String DEFAULT_FINANCIAL_YEAR = "2026-2027";
    private static final String DEFAULT_REGIME = "NEW";
    private static final String OLD_REGIME = "OLD";
    private static final BigDecimal DEFAULT_NEW_STANDARD_DEDUCTION = new BigDecimal("75000.00");
    private static final BigDecimal DEFAULT_OLD_STANDARD_DEDUCTION = new BigDecimal("50000.00");
    private static final BigDecimal DEFAULT_NEW_87A_LIMIT = new BigDecimal("1275000.00");
    private static final BigDecimal DEFAULT_OLD_87A_LIMIT = new BigDecimal("500000.00");
    private static final BigDecimal CESS_RATE = new BigDecimal("0.04");
    private static final BigDecimal HUNDRED = new BigDecimal("100");

    private final StatutoryQueryService statutoryQueryService;

    @Override
    public boolean supports(String statutoryType) {
        return STATUTORY_TYPE.equalsIgnoreCase(statutoryType);
    }

    @Override
    public void calculate(StatutoryCalculationRequest request,
                          StatutoryCalculationResult.StatutoryCalculationResultBuilder resultBuilder) {
        int month = request.getPayrollMonth();
        int remainingMonths = calculateRemainingMonths(month);

        BigDecimal earnedGross = request.getEarnedGross() != null ? request.getEarnedGross() : BigDecimal.ZERO;
        BigDecimal ytdEarnedGross = request.getYtdEarnedGross() != null ? request.getYtdEarnedGross() : BigDecimal.ZERO;
        BigDecimal prevEmployerGross = request.getPreviousEmployerGross() != null
                ? request.getPreviousEmployerGross()
                : BigDecimal.ZERO;

        BigDecimal projectedAnnualGross = earnedGross.multiply(BigDecimal.valueOf(remainingMonths))
                .add(ytdEarnedGross)
                .add(prevEmployerGross)
                .setScale(2, RoundingMode.HALF_UP);

        String regime = (request.getTaxRegime() != null && !request.getTaxRegime().isBlank())
                ? request.getTaxRegime().trim().toUpperCase()
                : DEFAULT_REGIME;

        String financialYear = resolveFinancialYear(request, month);

        List<TdsTaxSlab> slabs = statutoryQueryService.getTdsSlabs(financialYear, regime);
        if (slabs == null || slabs.isEmpty()) {
            slabs = statutoryQueryService.getTdsSlabs(DEFAULT_FINANCIAL_YEAR, regime);
        }

        BigDecimal standardDeduction = resolveStandardDeduction(slabs, regime);
        BigDecimal taxableIncome = projectedAnnualGross.subtract(standardDeduction)
                .max(BigDecimal.ZERO)
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal taxBeforeRebate = calculateProgressiveTax(taxableIncome, slabs);

        BigDecimal rebate87aLimit = resolveRebate87aLimit(slabs, regime);
        BigDecimal section87aRebate = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        if (taxableIncome.compareTo(rebate87aLimit) <= 0) {
            section87aRebate = taxBeforeRebate;
        }

        BigDecimal taxAfterRebate = taxBeforeRebate.subtract(section87aRebate)
                .max(BigDecimal.ZERO)
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal educationCess = taxAfterRebate.multiply(CESS_RATE).setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalAnnualTax = taxAfterRebate.add(educationCess).setScale(2, RoundingMode.HALF_UP);

        BigDecimal ytdTdsDeducted = request.getYtdTdsDeducted() != null ? request.getYtdTdsDeducted() : BigDecimal.ZERO;
        BigDecimal prevEmployerTds = request.getPreviousEmployerTds() != null
                ? request.getPreviousEmployerTds()
                : BigDecimal.ZERO;
        BigDecimal taxPaidSoFar = ytdTdsDeducted.add(prevEmployerTds).setScale(2, RoundingMode.HALF_UP);

        BigDecimal remainingTax = totalAnnualTax.subtract(taxPaidSoFar);
        BigDecimal monthlyTds = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        if (remainingTax.compareTo(BigDecimal.ZERO) > 0) {
            monthlyTds = remainingTax.divide(BigDecimal.valueOf(remainingMonths), 2, RoundingMode.HALF_UP);
        }
        monthlyTds = monthlyTds.max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);

        TdsCalculationResult result = TdsCalculationResult.builder()
                .financialYear(financialYear)
                .regime(regime)
                .projectedAnnualGross(projectedAnnualGross)
                .standardDeduction(standardDeduction)
                .taxableIncome(taxableIncome)
                .taxBeforeCess(taxBeforeRebate)
                .section87aRebate(section87aRebate)
                .taxAfterRebate(taxAfterRebate)
                .educationCess(educationCess)
                .totalAnnualTax(totalAnnualTax)
                .taxPaidSoFar(taxPaidSoFar)
                .remainingMonths(remainingMonths)
                .monthlyTdsDeduction(monthlyTds)
                .build();

        resultBuilder.tds(result);
    }

    private int calculateRemainingMonths(int payrollMonth) {
        int remaining;
        if (payrollMonth >= 4 && payrollMonth <= 12) {
            remaining = 13 - (payrollMonth - 3);
        } else if (payrollMonth >= 1 && payrollMonth <= 3) {
            remaining = 13 - (payrollMonth + 9);
        } else {
            remaining = 12;
        }
        return Math.max(1, remaining);
    }

    private String resolveFinancialYear(StatutoryCalculationRequest request, int payrollMonth) {
        if (request.getFinancialYear() != null && !request.getFinancialYear().isBlank()) {
            return request.getFinancialYear().trim();
        }
        int year = request.getPayrollYear() > 0 ? request.getPayrollYear() : LocalDate.now().getYear();
        if (payrollMonth >= 4) {
            return year + "-" + (year + 1);
        } else {
            return (year - 1) + "-" + year;
        }
    }

    private BigDecimal resolveStandardDeduction(List<TdsTaxSlab> slabs, String regime) {
        if (slabs != null && !slabs.isEmpty() && slabs.get(0).getStandardDeduction() != null) {
            return slabs.get(0).getStandardDeduction().setScale(2, RoundingMode.HALF_UP);
        }
        return OLD_REGIME.equalsIgnoreCase(regime)
                ? DEFAULT_OLD_STANDARD_DEDUCTION
                : DEFAULT_NEW_STANDARD_DEDUCTION;
    }

    private BigDecimal resolveRebate87aLimit(List<TdsTaxSlab> slabs, String regime) {
        if (slabs != null && !slabs.isEmpty() && slabs.get(0).getRebate87aLimit() != null) {
            return slabs.get(0).getRebate87aLimit().setScale(2, RoundingMode.HALF_UP);
        }
        return OLD_REGIME.equalsIgnoreCase(regime)
                ? DEFAULT_OLD_87A_LIMIT
                : DEFAULT_NEW_87A_LIMIT;
    }

    private BigDecimal calculateProgressiveTax(BigDecimal taxableIncome, List<TdsTaxSlab> slabs) {
        BigDecimal taxBeforeRebate = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        if (slabs == null || slabs.isEmpty()) {
            return taxBeforeRebate;
        }

        for (TdsTaxSlab slab : slabs) {
            BigDecimal lower = slab.getIncomeFrom().setScale(0, RoundingMode.FLOOR);
            BigDecimal upper = slab.getIncomeTo();
            if (taxableIncome.compareTo(lower) > 0) {
                BigDecimal taxableInSlab;
                if (upper != null) {
                    taxableInSlab = taxableIncome.min(upper).subtract(lower);
                } else {
                    taxableInSlab = taxableIncome.subtract(lower);
                }
                if (taxableInSlab.compareTo(BigDecimal.ZERO) > 0 && slab.getTaxRate().compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal slabTax = taxableInSlab.multiply(slab.getTaxRate())
                            .divide(HUNDRED, 2, RoundingMode.HALF_UP);
                    taxBeforeRebate = taxBeforeRebate.add(slabTax);
                }
            }
        }
        return taxBeforeRebate.setScale(2, RoundingMode.HALF_UP);
    }
}

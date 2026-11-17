package com.ourhr.statutory.engine.strategies;

import com.ourhr.statutory.PtSlab;
import com.ourhr.statutory.StatutoryQueryService;
import com.ourhr.statutory.engine.StatutoryCalculationStrategy;
import com.ourhr.statutory.engine.dto.PtCalculationResult;
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
public class PtCalculatorStrategy implements StatutoryCalculationStrategy {

    public static final String STATUTORY_TYPE = "PT";
    private static final String STATE_MAHARASHTRA = "MH";
    private static final String DEFAULT_STATE = "KA";
    private static final int FEBRUARY = 2;

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

        String gender = (request.getGender() != null && !request.getGender().isBlank())
                ? request.getGender().trim().toUpperCase()
                : "ALL";

        BigDecimal salary = request.getEarnedGross() != null ? request.getEarnedGross() : BigDecimal.ZERO;

        Optional<PtSlab> slabOpt = statutoryQueryService.getPtSlab(stateCode, gender, salary, queryDate);
        if (slabOpt.isEmpty() && !"ALL".equalsIgnoreCase(gender)) {
            slabOpt = statutoryQueryService.getPtSlab(stateCode, "ALL", salary, queryDate);
        }

        int month = request.getPayrollMonth();
        boolean isMh = STATE_MAHARASHTRA.equalsIgnoreCase(stateCode);
        boolean isFeb = (month == FEBRUARY);

        boolean isSpecialMonth = false;
        BigDecimal taxAmount = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

        if (slabOpt.isPresent()) {
            PtSlab slab = slabOpt.get();
            BigDecimal standardTax = slab.getTaxAmount() != null
                    ? slab.getTaxAmount().setScale(2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

            if (isMh && isFeb) {
                if (slab.getSpecialMonthTaxAmount() != null && standardTax.compareTo(BigDecimal.ZERO) > 0) {
                    taxAmount = slab.getSpecialMonthTaxAmount().setScale(2, RoundingMode.HALF_UP);
                    isSpecialMonth = true;
                } else {
                    taxAmount = standardTax;
                }
            } else if (slab.getSpecialMonth() != null && slab.getSpecialMonth().intValue() == month
                    && slab.getSpecialMonthTaxAmount() != null && standardTax.compareTo(BigDecimal.ZERO) > 0) {
                taxAmount = slab.getSpecialMonthTaxAmount().setScale(2, RoundingMode.HALF_UP);
                isSpecialMonth = true;
            } else {
                taxAmount = standardTax;
            }
        }

        PtCalculationResult result = PtCalculationResult.builder()
                .stateCode(stateCode)
                .salaryAmount(salary.setScale(2, RoundingMode.HALF_UP))
                .taxAmount(taxAmount)
                .isSpecialMonth(isSpecialMonth)
                .build();

        resultBuilder.pt(result);
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

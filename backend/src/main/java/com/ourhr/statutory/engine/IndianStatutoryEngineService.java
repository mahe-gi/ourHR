package com.ourhr.statutory.engine;

import com.ourhr.statutory.engine.dto.StatutoryCalculationRequest;
import com.ourhr.statutory.engine.dto.StatutoryCalculationResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
@RequiredArgsConstructor
public class IndianStatutoryEngineService {

    private final List<StatutoryCalculationStrategy> strategies;
    private final StatutoryStrategyFactory strategyFactory;

    public StatutoryCalculationResult calculate(StatutoryCalculationRequest request) {
        StatutoryCalculationResult.StatutoryCalculationResultBuilder builder =
                StatutoryCalculationResult.builder();

        for (StatutoryCalculationStrategy strategy : strategies) {
            strategy.calculate(request, builder);
        }

        StatutoryCalculationResult partial = builder.build();

        BigDecimal totalEmployeeDeductions = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalEmployerContributions = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

        if (partial.getEpf() != null) {
            if (partial.getEpf().getEmployeeContribution() != null) {
                totalEmployeeDeductions = totalEmployeeDeductions.add(partial.getEpf().getEmployeeContribution());
            }
            if (partial.getEpf().getTotalEmployerLiability() != null) {
                totalEmployerContributions = totalEmployerContributions.add(partial.getEpf().getTotalEmployerLiability());
            }
        }

        if (partial.getEsic() != null && partial.getEsic().isEligible()) {
            if (partial.getEsic().getEmployeeContribution() != null) {
                totalEmployeeDeductions = totalEmployeeDeductions.add(partial.getEsic().getEmployeeContribution());
            }
            if (partial.getEsic().getEmployerContribution() != null) {
                totalEmployerContributions = totalEmployerContributions.add(partial.getEsic().getEmployerContribution());
            }
        }

        if (partial.getPt() != null && partial.getPt().getTaxAmount() != null) {
            totalEmployeeDeductions = totalEmployeeDeductions.add(partial.getPt().getTaxAmount());
        }

        if (partial.getLwf() != null && partial.getLwf().isApplicable()) {
            if (partial.getLwf().getEmployeeContribution() != null) {
                totalEmployeeDeductions = totalEmployeeDeductions.add(partial.getLwf().getEmployeeContribution());
            }
            if (partial.getLwf().getEmployerContribution() != null) {
                totalEmployerContributions = totalEmployerContributions.add(partial.getLwf().getEmployerContribution());
            }
        }

        if (partial.getTds() != null && partial.getTds().getMonthlyTdsDeduction() != null) {
            totalEmployeeDeductions = totalEmployeeDeductions.add(partial.getTds().getMonthlyTdsDeduction());
        }

        return builder
                .totalEmployeeDeductions(totalEmployeeDeductions.setScale(2, RoundingMode.HALF_UP))
                .totalEmployerContributions(totalEmployerContributions.setScale(2, RoundingMode.HALF_UP))
                .build();
    }

    public StatutoryCalculationResult calculateType(String statutoryType, StatutoryCalculationRequest request) {
        StatutoryCalculationResult.StatutoryCalculationResultBuilder builder =
                StatutoryCalculationResult.builder();

        strategyFactory.getStrategy(statutoryType)
                .ifPresent(strategy -> strategy.calculate(request, builder));

        return builder.build();
    }
}

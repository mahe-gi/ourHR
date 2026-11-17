package com.ourhr.statutory.engine;

import com.ourhr.statutory.engine.dto.StatutoryCalculationRequest;
import com.ourhr.statutory.engine.dto.StatutoryCalculationResult;

public interface StatutoryCalculationStrategy {

    boolean supports(String statutoryType);

    void calculate(StatutoryCalculationRequest request,
                   StatutoryCalculationResult.StatutoryCalculationResultBuilder resultBuilder);
}

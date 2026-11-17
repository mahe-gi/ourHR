package com.ourhr.statutory.engine;

import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Component
public class StatutoryStrategyFactory {

    private final List<StatutoryCalculationStrategy> strategies;

    public StatutoryStrategyFactory(List<StatutoryCalculationStrategy> strategies) {
        this.strategies = strategies != null ? strategies : Collections.emptyList();
    }

    public List<StatutoryCalculationStrategy> getAllStrategies() {
        return Collections.unmodifiableList(strategies);
    }

    public Optional<StatutoryCalculationStrategy> getStrategy(String statutoryType) {
        if (statutoryType == null) {
            return Optional.empty();
        }
        return strategies.stream()
                .filter(strategy -> strategy.supports(statutoryType))
                .findFirst();
    }
}

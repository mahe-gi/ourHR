package com.ourhr.statutory;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface StatutoryRuleRepository extends JpaRepository<StatutoryRule, UUID> {

    @Query("SELECT r FROM StatutoryRule r WHERE r.ruleKey = :ruleKey AND r.effectiveFrom <= :onDate AND (r.effectiveTo IS NULL OR r.effectiveTo >= :onDate)")
    Optional<StatutoryRule> findEffectiveRule(@Param("ruleKey") String ruleKey, @Param("onDate") LocalDate onDate);
}

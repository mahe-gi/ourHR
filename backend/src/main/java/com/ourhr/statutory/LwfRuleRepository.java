package com.ourhr.statutory;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LwfRuleRepository extends JpaRepository<LwfRule, UUID> {

    @Query("SELECT l FROM LwfRule l WHERE l.stateCode = :stateCode AND l.effectiveFrom <= :onDate AND (l.effectiveTo IS NULL OR l.effectiveTo >= :onDate)")
    Optional<LwfRule> findEffectiveRule(@Param("stateCode") String stateCode, @Param("onDate") LocalDate onDate);
}

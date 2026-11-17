package com.ourhr.statutory;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PtSlabRepository extends JpaRepository<PtSlab, UUID> {

    @Query("SELECT p FROM PtSlab p WHERE p.stateCode = :stateCode AND (p.gender = 'ALL' OR p.gender = :gender) AND p.salaryFrom <= :salary AND (p.salaryTo IS NULL OR p.salaryTo >= :salary) AND p.effectiveFrom <= :onDate AND (p.effectiveTo IS NULL OR p.effectiveTo >= :onDate)")
    Optional<PtSlab> findApplicableSlab(
            @Param("stateCode") String stateCode,
            @Param("gender") String gender,
            @Param("salary") BigDecimal salary,
            @Param("onDate") LocalDate onDate
    );

    @Query("SELECT p FROM PtSlab p WHERE p.stateCode = :stateCode AND p.effectiveFrom <= :onDate AND (p.effectiveTo IS NULL OR p.effectiveTo >= :onDate) ORDER BY p.salaryFrom ASC")
    List<PtSlab> findSlabsByState(@Param("stateCode") String stateCode, @Param("onDate") LocalDate onDate);
}

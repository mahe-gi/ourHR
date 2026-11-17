package com.ourhr.statutory;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TdsTaxSlabRepository extends JpaRepository<TdsTaxSlab, UUID> {

    List<TdsTaxSlab> findByFinancialYearAndRegimeOrderByIncomeFromAsc(String financialYear, String regime);
}

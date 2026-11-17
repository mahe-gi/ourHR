package com.ourhr.company;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BranchRepository extends JpaRepository<Branch, UUID> {

    List<Branch> findByCompanyId(UUID companyId);

    Optional<Branch> findByCompanyIdAndIsHeadquartersTrue(UUID companyId);

    Optional<Branch> findByIdAndCompanyId(UUID id, UUID companyId);

    List<Branch> findByCompanyIdAndStateCode(UUID companyId, String stateCode);
}

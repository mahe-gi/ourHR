package com.ourhr.company;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CompanyRepository extends JpaRepository<Company, UUID> {

    Optional<Company> findByPanNumber(String panNumber);

    boolean existsByPanNumber(String panNumber);

    boolean existsByTanNumber(String tanNumber);
}

package com.ourhr.statutory.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StatutoryRuleDto {

    private UUID id;
    private String ruleKey;
    private BigDecimal ruleValue;
    private String ruleMetadata;
    private LocalDate effectiveFrom;
    private LocalDate effectiveTo;
    private String description;
}

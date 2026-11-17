package com.ourhr.employee.salary;

import com.ourhr.employee.salary.dto.SalaryOfferRequest;
import com.ourhr.employee.salary.dto.SalaryOfferValidationResult;
import com.ourhr.statutory.StatutoryQueryService;
import com.ourhr.statutory.StatutoryRule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BasicWageValidatorTest {

    @Mock
    private StatutoryQueryService statutoryQueryService;

    @InjectMocks
    private BasicWageValidator basicWageValidator;

    private LocalDate testEffectiveDate;

    @BeforeEach
    void setUp() {
        testEffectiveDate = LocalDate.of(2026, 4, 1);
    }

    @Test
    @DisplayName("Valid salary offer where Basic + DA equals or exceeds 50% of gross")
    void shouldPassValidationWhenBasicPlusDaExceedsFiftyPercent() {
        StatutoryRule rule = StatutoryRule.builder()
                .ruleKey(StatutoryQueryService.RULE_KEY_BASIC_WAGE_RATIO_MIN)
                .ruleValue(new BigDecimal("0.50"))
                .effectiveFrom(LocalDate.of(2020, 1, 1))
                .build();

        when(statutoryQueryService.getEffectiveRule(eq(StatutoryQueryService.RULE_KEY_BASIC_WAGE_RATIO_MIN), eq(testEffectiveDate)))
                .thenReturn(Optional.of(rule));

        SalaryOfferRequest request = SalaryOfferRequest.builder()
                .monthlyGross(new BigDecimal("100000.00"))
                .basicPay(new BigDecimal("50000.00"))
                .daPay(new BigDecimal("10000.00"))
                .effectiveFrom(testEffectiveDate)
                .build();

        SalaryOfferValidationResult result = basicWageValidator.validate(request);

        assertThat(result.isValid()).isTrue();
        assertThat(result.getBasicPlusDa()).isEqualByComparingTo(new BigDecimal("60000.00"));
        assertThat(result.getMonthlyGross()).isEqualByComparingTo(new BigDecimal("100000.00"));
        assertThat(result.getWageRatio()).isEqualByComparingTo(new BigDecimal("0.6000"));
        assertThat(result.getMinRequiredRatio()).isEqualByComparingTo(new BigDecimal("0.50"));
        assertThat(result.getStatutoryDeficit()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.getMessage()).contains("Compliant with Code on Wages 2019");

        verify(statutoryQueryService).getEffectiveRule(StatutoryQueryService.RULE_KEY_BASIC_WAGE_RATIO_MIN, testEffectiveDate);
    }

    @Test
    @DisplayName("Invalid salary offer where Basic + DA is below 50% of gross, calculating statutory deficit")
    void shouldFailValidationWhenBasicPlusDaIsBelowFiftyPercent() {
        StatutoryRule rule = StatutoryRule.builder()
                .ruleKey(StatutoryQueryService.RULE_KEY_BASIC_WAGE_RATIO_MIN)
                .ruleValue(new BigDecimal("0.50"))
                .effectiveFrom(LocalDate.of(2020, 1, 1))
                .build();

        when(statutoryQueryService.getEffectiveRule(eq(StatutoryQueryService.RULE_KEY_BASIC_WAGE_RATIO_MIN), eq(testEffectiveDate)))
                .thenReturn(Optional.of(rule));

        SalaryOfferRequest request = SalaryOfferRequest.builder()
                .monthlyGross(new BigDecimal("100000.00"))
                .basicPay(new BigDecimal("30000.00"))
                .daPay(BigDecimal.ZERO)
                .effectiveFrom(testEffectiveDate)
                .build();

        SalaryOfferValidationResult result = basicWageValidator.validate(request);

        assertThat(result.isValid()).isFalse();
        assertThat(result.getBasicPlusDa()).isEqualByComparingTo(new BigDecimal("30000.00"));
        assertThat(result.getMonthlyGross()).isEqualByComparingTo(new BigDecimal("100000.00"));
        assertThat(result.getWageRatio()).isEqualByComparingTo(new BigDecimal("0.3000"));
        assertThat(result.getMinRequiredRatio()).isEqualByComparingTo(new BigDecimal("0.50"));
        assertThat(result.getStatutoryDeficit()).isEqualByComparingTo(new BigDecimal("20000.00"));
        assertThat(result.getMessage()).contains("Code on Wages 2019 violation");
    }

    @Test
    @DisplayName("Exact 50% boundary check should pass with zero deficit")
    void shouldPassExactFiftyPercentBoundary() {
        StatutoryRule rule = StatutoryRule.builder()
                .ruleKey(StatutoryQueryService.RULE_KEY_BASIC_WAGE_RATIO_MIN)
                .ruleValue(new BigDecimal("0.50"))
                .effectiveFrom(LocalDate.of(2020, 1, 1))
                .build();

        when(statutoryQueryService.getEffectiveRule(eq(StatutoryQueryService.RULE_KEY_BASIC_WAGE_RATIO_MIN), eq(testEffectiveDate)))
                .thenReturn(Optional.of(rule));

        SalaryOfferRequest request = SalaryOfferRequest.builder()
                .monthlyGross(new BigDecimal("100000.00"))
                .basicPay(new BigDecimal("50000.00"))
                .daPay(BigDecimal.ZERO)
                .effectiveFrom(testEffectiveDate)
                .build();

        SalaryOfferValidationResult result = basicWageValidator.validate(request);

        assertThat(result.isValid()).isTrue();
        assertThat(result.getWageRatio()).isEqualByComparingTo(new BigDecimal("0.5000"));
        assertThat(result.getStatutoryDeficit()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("Fallback to default 0.50 ratio when statutoryQueryService returns empty")
    void shouldFallbackToDefaultRatioWhenStatutoryRuleNotFound() {
        when(statutoryQueryService.getEffectiveRule(any(), any()))
                .thenReturn(Optional.empty());

        SalaryOfferRequest request = SalaryOfferRequest.builder()
                .monthlyGross(new BigDecimal("50000.00"))
                .basicPay(new BigDecimal("25000.00"))
                .daPay(BigDecimal.ZERO)
                .effectiveFrom(testEffectiveDate)
                .build();

        SalaryOfferValidationResult result = basicWageValidator.validate(request);

        assertThat(result.isValid()).isTrue();
        assertThat(result.getMinRequiredRatio()).isEqualByComparingTo(new BigDecimal("0.50"));
        assertThat(result.getStatutoryDeficit()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("Dynamic rule lookup with configured higher statutory ratio (e.g. 55%)")
    void shouldRespectDynamicRatioFromStatutoryRule() {
        StatutoryRule rule = StatutoryRule.builder()
                .ruleKey(StatutoryQueryService.RULE_KEY_BASIC_WAGE_RATIO_MIN)
                .ruleValue(new BigDecimal("0.55"))
                .effectiveFrom(LocalDate.of(2026, 1, 1))
                .build();

        when(statutoryQueryService.getEffectiveRule(eq(StatutoryQueryService.RULE_KEY_BASIC_WAGE_RATIO_MIN), eq(testEffectiveDate)))
                .thenReturn(Optional.of(rule));

        SalaryOfferRequest request = SalaryOfferRequest.builder()
                .monthlyGross(new BigDecimal("100000.00"))
                .basicPay(new BigDecimal("52000.00"))
                .daPay(BigDecimal.ZERO)
                .effectiveFrom(testEffectiveDate)
                .build();

        SalaryOfferValidationResult result = basicWageValidator.validate(request);

        assertThat(result.isValid()).isFalse();
        assertThat(result.getMinRequiredRatio()).isEqualByComparingTo(new BigDecimal("0.55"));
        assertThat(result.getStatutoryDeficit()).isEqualByComparingTo(new BigDecimal("3000.00"));
    }

    @Test
    @DisplayName("validateOrThrow throws IllegalArgumentException on invalid offer")
    void shouldThrowIllegalArgumentExceptionOnInvalidOffer() {
        StatutoryRule rule = StatutoryRule.builder()
                .ruleKey(StatutoryQueryService.RULE_KEY_BASIC_WAGE_RATIO_MIN)
                .ruleValue(new BigDecimal("0.50"))
                .effectiveFrom(LocalDate.of(2020, 1, 1))
                .build();

        when(statutoryQueryService.getEffectiveRule(eq(StatutoryQueryService.RULE_KEY_BASIC_WAGE_RATIO_MIN), eq(testEffectiveDate)))
                .thenReturn(Optional.of(rule));

        SalaryOfferRequest request = SalaryOfferRequest.builder()
                .monthlyGross(new BigDecimal("100000.00"))
                .basicPay(new BigDecimal("35000.00"))
                .daPay(BigDecimal.ZERO)
                .effectiveFrom(testEffectiveDate)
                .build();

        assertThatThrownBy(() -> basicWageValidator.validateOrThrow(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Code on Wages 2019 violation");
    }

    @Test
    @DisplayName("validateOrThrow does not throw when offer is compliant")
    void shouldNotThrowWhenOfferIsCompliant() {
        StatutoryRule rule = StatutoryRule.builder()
                .ruleKey(StatutoryQueryService.RULE_KEY_BASIC_WAGE_RATIO_MIN)
                .ruleValue(new BigDecimal("0.50"))
                .effectiveFrom(LocalDate.of(2020, 1, 1))
                .build();

        when(statutoryQueryService.getEffectiveRule(eq(StatutoryQueryService.RULE_KEY_BASIC_WAGE_RATIO_MIN), eq(testEffectiveDate)))
                .thenReturn(Optional.of(rule));

        SalaryOfferRequest request = SalaryOfferRequest.builder()
                .monthlyGross(new BigDecimal("100000.00"))
                .basicPay(new BigDecimal("55000.00"))
                .daPay(BigDecimal.ZERO)
                .effectiveFrom(testEffectiveDate)
                .build();

        assertThatCode(() -> basicWageValidator.validateOrThrow(request))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Null request should throw IllegalArgumentException")
    void shouldThrowOnNullRequest() {
        assertThatThrownBy(() -> basicWageValidator.validate(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Salary offer request cannot be null");
    }

    @Test
    @DisplayName("Zero or negative monthly gross should throw IllegalArgumentException")
    void shouldThrowOnInvalidMonthlyGross() {
        SalaryOfferRequest zeroGross = SalaryOfferRequest.builder()
                .monthlyGross(BigDecimal.ZERO)
                .basicPay(new BigDecimal("10000.00"))
                .build();

        assertThatThrownBy(() -> basicWageValidator.validate(zeroGross))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Monthly gross must be greater than zero");

        SalaryOfferRequest negativeGross = SalaryOfferRequest.builder()
                .monthlyGross(new BigDecimal("-5000.00"))
                .basicPay(new BigDecimal("10000.00"))
                .build();

        assertThatThrownBy(() -> basicWageValidator.validate(negativeGross))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Monthly gross must be greater than zero");
    }
}

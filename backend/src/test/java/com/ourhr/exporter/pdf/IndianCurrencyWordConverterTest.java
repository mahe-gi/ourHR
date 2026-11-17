package com.ourhr.exporter.pdf;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class IndianCurrencyWordConverterTest {

    @Test
    @DisplayName("Convert zero amount returns Rupees Zero Only")
    void testZeroAmount() {
        assertThat(IndianCurrencyWordConverter.convertToWords(BigDecimal.ZERO))
                .isEqualTo("Rupees Zero Only");
        assertThat(IndianCurrencyWordConverter.convertToWords(new BigDecimal("0.00")))
                .isEqualTo("Rupees Zero Only");
        assertThat(IndianCurrencyWordConverter.convertToWords(null))
                .isEqualTo("Rupees Zero Only");
    }

    @Test
    @DisplayName("Convert units and tens correctly")
    void testUnitsAndTens() {
        assertThat(IndianCurrencyWordConverter.convertToWords(new BigDecimal("5.00")))
                .isEqualTo("Rupees Five Only");
        assertThat(IndianCurrencyWordConverter.convertToWords(new BigDecimal("15.00")))
                .isEqualTo("Rupees Fifteen Only");
        assertThat(IndianCurrencyWordConverter.convertToWords(new BigDecimal("45.00")))
                .isEqualTo("Rupees Forty Five Only");
    }

    @Test
    @DisplayName("Convert hundreds and thousands correctly")
    void testHundredsAndThousands() {
        assertThat(IndianCurrencyWordConverter.convertToWords(new BigDecimal("500.00")))
                .isEqualTo("Rupees Five Hundred Only");
        assertThat(IndianCurrencyWordConverter.convertToWords(new BigDecimal("1500.00")))
                .isEqualTo("Rupees One Thousand Five Hundred Only");
        assertThat(IndianCurrencyWordConverter.convertToWords(new BigDecimal("25450.00")))
                .isEqualTo("Rupees Twenty Five Thousand Four Hundred Fifty Only");
    }

    @Test
    @DisplayName("Convert lakhs and crores correctly")
    void testLakhsAndCrores() {
        assertThat(IndianCurrencyWordConverter.convertToWords(new BigDecimal("100000.00")))
                .isEqualTo("Rupees One Lakh Only");
        assertThat(IndianCurrencyWordConverter.convertToWords(new BigDecimal("1575000.00")))
                .isEqualTo("Rupees Fifteen Lakh Seventy Five Thousand Only");
        assertThat(IndianCurrencyWordConverter.convertToWords(new BigDecimal("25000000.00")))
                .isEqualTo("Rupees Two Crore Fifty Lakh Only");
    }

    @Test
    @DisplayName("Convert amounts with paise correctly")
    void testAmountsWithPaise() {
        assertThat(IndianCurrencyWordConverter.convertToWords(new BigDecimal("25450.50")))
                .isEqualTo("Rupees Twenty Five Thousand Four Hundred Fifty and Fifty Paise Only");
        assertThat(IndianCurrencyWordConverter.convertToWords(new BigDecimal("0.75")))
                .isEqualTo("Rupees Zero and Seventy Five Paise Only");
    }
}

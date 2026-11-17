package com.ourhr.exporter.pdf;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Converts financial amounts in Indian Rupees (INR) to words following the Indian numbering system.
 * (Crores, Lakhs, Thousands, Hundreds).
 */
public final class IndianCurrencyWordConverter {

    private static final String[] UNITS = {
            "", "One", "Two", "Three", "Four", "Five", "Six", "Seven", "Eight", "Nine", "Ten",
            "Eleven", "Twelve", "Thirteen", "Fourteen", "Fifteen", "Sixteen", "Seventeen", "Eighteen", "Nineteen"
    };

    private static final String[] TENS = {
            "", "", "Twenty", "Thirty", "Forty", "Fifty", "Sixty", "Seventy", "Eighty", "Ninety"
    };

    private IndianCurrencyWordConverter() {
    }

    public static String convertToWords(BigDecimal amount) {
        if (amount == null) {
            return "Rupees Zero Only";
        }

        BigDecimal scaled = amount.setScale(2, RoundingMode.HALF_UP);
        long rupees = scaled.longValue();
        int paise = scaled.remainder(BigDecimal.ONE).multiply(new BigDecimal("100")).intValue();

        if (rupees == 0 && paise == 0) {
            return "Rupees Zero Only";
        }

        StringBuilder sb = new StringBuilder("Rupees ");
        if (rupees > 0) {
            sb.append(convertWholeNumber(rupees));
        } else {
            sb.append("Zero");
        }

        if (paise > 0) {
            sb.append(" and ").append(convertTwoDigits(paise)).append(" Paise");
        }

        sb.append(" Only");
        return sb.toString();
    }

    private static String convertWholeNumber(long n) {
        if (n == 0) {
            return "Zero";
        }

        long crores = n / 10000000L;
        long remainder = n % 10000000L;

        long lakhs = remainder / 100000L;
        remainder = remainder % 100000L;

        long thousands = remainder / 1000L;
        remainder = remainder % 1000L;

        long hundreds = remainder;

        StringBuilder result = new StringBuilder();

        if (crores > 0) {
            result.append(convertWholeNumber(crores)).append(" Crore ");
        }

        if (lakhs > 0) {
            result.append(convertTwoDigits((int) lakhs)).append(" Lakh ");
        }

        if (thousands > 0) {
            result.append(convertTwoDigits((int) thousands)).append(" Thousand ");
        }

        if (hundreds > 0) {
            result.append(convertThreeDigits((int) hundreds)).append(" ");
        }

        return result.toString().trim();
    }

    private static String convertThreeDigits(int n) {
        if (n == 0) {
            return "";
        }
        if (n < 100) {
            return convertTwoDigits(n);
        }
        int h = n / 100;
        int rem = n % 100;
        if (rem == 0) {
            return UNITS[h] + " Hundred";
        }
        return UNITS[h] + " Hundred " + convertTwoDigits(rem);
    }

    private static String convertTwoDigits(int n) {
        if (n < 20) {
            return UNITS[n];
        }
        int t = n / 10;
        int u = n % 10;
        if (u == 0) {
            return TENS[t];
        }
        return TENS[t] + " " + UNITS[u];
    }
}

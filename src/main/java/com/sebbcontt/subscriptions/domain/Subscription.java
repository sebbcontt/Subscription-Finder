package com.sebbcontt.subscriptions.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * A recurring charge found in the data.
 *
 * @param priceChangePercent change from the usual earlier amount to the latest charge, or null if the price did not move
 * @param active false when the next charge is overdue, which usually means it was cancelled
 */
public record Subscription(
        String merchant,
        Cadence cadence,
        BigDecimal lastAmount,
        BigDecimal monthlyCost,
        BigDecimal yearlyCost,
        int occurrences,
        LocalDate firstSeen,
        LocalDate lastSeen,
        LocalDate nextExpected,
        boolean active,
        BigDecimal priceChangePercent) {
}

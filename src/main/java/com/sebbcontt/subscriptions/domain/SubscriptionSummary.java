package com.sebbcontt.subscriptions.domain;

import java.math.BigDecimal;
import java.util.List;

/**
 * Totals across the subscriptions that are still active.
 *
 * @param priceIncreases active subscriptions whose latest charge cost more than before
 * @param possiblyCancelled subscriptions whose next charge is overdue
 */
public record SubscriptionSummary(
        int activeCount,
        BigDecimal monthlyTotal,
        BigDecimal yearlyTotal,
        List<Subscription> priceIncreases,
        List<Subscription> possiblyCancelled) {

    public static SubscriptionSummary of(List<Subscription> subscriptions) {
        List<Subscription> active = subscriptions.stream().filter(Subscription::active).toList();
        BigDecimal monthly = active.stream().map(Subscription::monthlyCost).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal yearly = active.stream().map(Subscription::yearlyCost).reduce(BigDecimal.ZERO, BigDecimal::add);
        List<Subscription> increases = active.stream()
                .filter(s -> s.priceChangePercent() != null && s.priceChangePercent().signum() > 0)
                .toList();
        List<Subscription> cancelled = subscriptions.stream().filter(s -> !s.active()).toList();
        return new SubscriptionSummary(active.size(), monthly, yearly, increases, cancelled);
    }
}

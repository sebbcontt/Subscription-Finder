package com.sebbcontt.subscriptions.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Finds recurring charges. A merchant counts as a subscription when it has at least
 * three charges, the gaps between them fit weekly, monthly, quarterly or yearly billing,
 * and the amounts stay close to each other.
 *
 * Groceries and coffee can come at a regular pace too, but their amounts jump around,
 * so the amount check keeps them out.
 */
public final class RecurringDetector {

    static final int MIN_OCCURRENCES = 3;
    /** A charge is "close" if it is within this share of the usual amount. */
    static final double AMOUNT_TOLERANCE = 0.10;
    /** Share of charges that must be close to the usual amount. */
    static final double MIN_AMOUNT_STABILITY = 0.75;
    /** Share of gaps that must fit the billing schedule. */
    static final double MIN_GAP_REGULARITY = 0.75;
    /** Smallest change from the earlier amount that is reported as a price change. */
    static final double PRICE_CHANGE_THRESHOLD_PERCENT = 3.0;

    public List<Subscription> detect(List<Charge> charges) {
        if (charges.isEmpty()) {
            return List.of();
        }
        LocalDate latestDate = charges.stream().map(Charge::date).max(Comparator.naturalOrder()).orElseThrow();

        Map<String, List<Charge>> byMerchant = new LinkedHashMap<>();
        for (Charge charge : charges) {
            String key = MerchantNormalizer.normalize(charge.description());
            if (!key.isEmpty()) {
                byMerchant.computeIfAbsent(key, k -> new ArrayList<>()).add(charge);
            }
        }

        List<Subscription> found = new ArrayList<>();
        byMerchant.forEach((merchant, group) -> analyze(merchant, group, latestDate).ifPresent(found::add));

        found.sort(Comparator.comparing(Subscription::active).reversed()
                .thenComparing(Subscription::monthlyCost, Comparator.reverseOrder())
                .thenComparing(Subscription::merchant));
        return found;
    }

    private Optional<Subscription> analyze(String merchant, List<Charge> group, LocalDate latestDate) {
        if (group.size() < MIN_OCCURRENCES) {
            return Optional.empty();
        }
        List<Charge> sorted = group.stream().sorted(Comparator.comparing(Charge::date)).toList();

        List<Long> gaps = new ArrayList<>();
        for (int i = 1; i < sorted.size(); i++) {
            gaps.add(ChronoUnit.DAYS.between(sorted.get(i - 1).date(), sorted.get(i).date()));
        }
        Optional<Cadence> maybeCadence = Cadence.fromMedianGap(median(gaps.stream().map(BigDecimal::valueOf).toList()).doubleValue());
        if (maybeCadence.isEmpty()) {
            return Optional.empty();
        }
        Cadence cadence = maybeCadence.get();

        long onSchedule = gaps.stream()
                .filter(g -> Math.abs(g - cadence.typicalDays) <= cadence.toleranceDays)
                .count();
        if ((double) onSchedule / gaps.size() < MIN_GAP_REGULARITY) {
            return Optional.empty();
        }

        List<BigDecimal> amounts = sorted.stream().map(Charge::amount).toList();
        if (!amountsAreStable(amounts)) {
            return Optional.empty();
        }

        Charge last = sorted.get(sorted.size() - 1);
        LocalDate lastSeen = last.date();
        LocalDate nextExpected = addOneCycle(lastSeen, cadence);
        boolean active = !latestDate.isAfter(nextExpected.plusDays(cadence.toleranceDays * 2L));

        BigDecimal earlierUsual = median(amounts.subList(0, amounts.size() - 1));
        BigDecimal priceChange = priceChangePercent(earlierUsual, last.amount());

        BigDecimal yearly = last.amount().multiply(BigDecimal.valueOf(cadence.chargesPerYear)).setScale(2, RoundingMode.HALF_UP);
        BigDecimal monthly = yearly.divide(BigDecimal.valueOf(12), 2, RoundingMode.HALF_UP);

        return Optional.of(new Subscription(
                MerchantNormalizer.toDisplayName(merchant), cadence, last.amount().setScale(2, RoundingMode.HALF_UP),
                monthly, yearly, sorted.size(), sorted.get(0).date(), lastSeen, nextExpected, active, priceChange));
    }

    /**
     * True when the amounts stay near one value, or near one value and then near a second
     * value after a single price change (for example 15.49 for four months, then 17.99).
     */
    private static boolean amountsAreStable(List<BigDecimal> amounts) {
        BigDecimal usual = median(amounts);
        long close = amounts.stream().filter(a -> isClose(a, usual)).count();
        if ((double) close / amounts.size() >= MIN_AMOUNT_STABILITY) {
            return true;
        }
        // Look for one price change: everything before it is steady, and everything after it is steady.
        for (int split = 2; split < amounts.size(); split++) {
            if (allClose(amounts.subList(0, split)) && allClose(amounts.subList(split, amounts.size()))) {
                return true;
            }
        }
        return false;
    }

    private static boolean allClose(List<BigDecimal> amounts) {
        BigDecimal usual = median(amounts);
        return amounts.stream().allMatch(a -> isClose(a, usual));
    }

    private static boolean isClose(BigDecimal amount, BigDecimal usual) {
        BigDecimal allowed = usual.multiply(BigDecimal.valueOf(AMOUNT_TOLERANCE));
        return amount.subtract(usual).abs().compareTo(allowed) <= 0;
    }

    private static BigDecimal priceChangePercent(BigDecimal before, BigDecimal now) {
        if (before.signum() == 0) {
            return null;
        }
        BigDecimal percent = now.subtract(before)
                .multiply(BigDecimal.valueOf(100))
                .divide(before, 1, RoundingMode.HALF_UP);
        return percent.abs().doubleValue() >= PRICE_CHANGE_THRESHOLD_PERCENT ? percent : null;
    }

    private static LocalDate addOneCycle(LocalDate date, Cadence cadence) {
        return switch (cadence) {
            case WEEKLY -> date.plusWeeks(1);
            case MONTHLY -> date.plusMonths(1);
            case QUARTERLY -> date.plusMonths(3);
            case YEARLY -> date.plusYears(1);
        };
    }

    private static BigDecimal median(List<BigDecimal> values) {
        List<BigDecimal> sorted = values.stream().sorted().toList();
        int n = sorted.size();
        if (n % 2 == 1) {
            return sorted.get(n / 2);
        }
        return sorted.get(n / 2 - 1).add(sorted.get(n / 2)).divide(BigDecimal.valueOf(2), 4, RoundingMode.HALF_UP);
    }
}

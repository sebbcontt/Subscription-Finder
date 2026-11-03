package com.sebbcontt.subscriptions.domain;

import java.util.Optional;

/** How often a subscription bills. */
public enum Cadence {
    WEEKLY(7, 2, 52, 5, 9),
    MONTHLY(30, 5, 12, 25, 35),
    QUARTERLY(91, 10, 4, 80, 100),
    YEARLY(365, 15, 1, 350, 380);

    /** Usual days between charges. */
    public final int typicalDays;
    /** How many days early or late a charge can be and still count as on schedule. */
    public final int toleranceDays;
    public final int chargesPerYear;
    private final int minMedianGap;
    private final int maxMedianGap;

    Cadence(int typicalDays, int toleranceDays, int chargesPerYear, int minMedianGap, int maxMedianGap) {
        this.typicalDays = typicalDays;
        this.toleranceDays = toleranceDays;
        this.chargesPerYear = chargesPerYear;
        this.minMedianGap = minMedianGap;
        this.maxMedianGap = maxMedianGap;
    }

    /** Picks the cadence whose range contains the median gap between charges, if any. */
    public static Optional<Cadence> fromMedianGap(double medianGapDays) {
        for (Cadence cadence : values()) {
            if (medianGapDays >= cadence.minMedianGap && medianGapDays <= cadence.maxMedianGap) {
                return Optional.of(cadence);
            }
        }
        return Optional.empty();
    }
}

package com.sebbcontt.subscriptions.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class RecurringDetectorTest {

    private final RecurringDetector detector = new RecurringDetector();

    private static Charge charge(String date, String description, String amount) {
        return new Charge(LocalDate.parse(date), description, new BigDecimal(amount));
    }

    /** Same description and amount on the same day of each month, starting from firstMonth (yyyy-MM). */
    private static List<Charge> monthly(String description, String amount, int day, String firstMonth, int months) {
        List<Charge> out = new ArrayList<>();
        LocalDate start = LocalDate.parse(firstMonth + "-" + String.format("%02d", day));
        for (int i = 0; i < months; i++) {
            out.add(new Charge(start.plusMonths(i), description, new BigDecimal(amount)));
        }
        return out;
    }

    @Test
    void findsAMonthlySubscriptionAndWorksOutCosts() {
        List<Subscription> found = detector.detect(monthly("NETFLIX.COM 866-579-7172 CA", "15.49", 3, "2026-01", 6));

        assertEquals(1, found.size());
        Subscription netflix = found.get(0);
        assertEquals("Netflix", netflix.merchant());
        assertEquals(Cadence.MONTHLY, netflix.cadence());
        assertEquals(6, netflix.occurrences());
        assertEquals(new BigDecimal("15.49"), netflix.lastAmount());
        assertEquals(new BigDecimal("185.88"), netflix.yearlyCost());
        assertEquals(new BigDecimal("15.49"), netflix.monthlyCost());
        assertEquals(LocalDate.of(2026, 6, 3), netflix.lastSeen());
        assertEquals(LocalDate.of(2026, 7, 3), netflix.nextExpected());
        assertTrue(netflix.active());
        assertNull(netflix.priceChangePercent());
    }

    @Test
    void sameMerchantWithChangingReferenceNumbersIsOneSubscription() {
        List<Charge> charges = new ArrayList<>();
        charges.add(charge("2026-01-08", "SPOTIFY USA 1111 NY", "11.99"));
        charges.add(charge("2026-02-08", "SPOTIFY USA 2222 NY", "11.99"));
        charges.add(charge("2026-03-08", "SPOTIFY USA 3333 NY", "11.99"));

        assertEquals(1, detector.detect(charges).size());
    }

    @Test
    void reportsAPriceIncrease() {
        List<Charge> charges = monthly("NETFLIX.COM", "15.49", 3, "2026-01", 4);
        charges.addAll(monthly("NETFLIX.COM", "17.99", 3, "2026-05", 2));

        Subscription netflix = detector.detect(charges).get(0);

        assertEquals(new BigDecimal("17.99"), netflix.lastAmount());
        assertNotNull(netflix.priceChangePercent());
        assertEquals(new BigDecimal("16.1"), netflix.priceChangePercent());
    }

    @Test
    void tinyPriceDifferencesAreNotReported() {
        List<Charge> charges = monthly("GYM", "39.00", 1, "2026-01", 4);
        charges.add(charge("2026-05-01", "GYM", "39.50"));
        assertNull(detector.detect(charges).get(0).priceChangePercent());
    }

    @Test
    void reportsAPriceDrop() {
        List<Charge> charges = monthly("ADOBE", "22.99", 10, "2026-01", 4);
        charges.add(charge("2026-05-10", "ADOBE", "19.99"));
        assertTrue(detector.detect(charges).get(0).priceChangePercent().signum() < 0);
    }

    @Test
    void findsWeeklyQuarterlyAndYearlyBilling() {
        List<Charge> weekly = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            weekly.add(new Charge(LocalDate.of(2026, 1, 5).plusWeeks(i), "MEAL KIT", new BigDecimal("59.94")));
        }
        assertEquals(Cadence.WEEKLY, detector.detect(weekly).get(0).cadence());
        assertEquals(new BigDecimal("259.74"), detector.detect(weekly).get(0).monthlyCost());

        List<Charge> quarterly = List.of(
                charge("2025-01-15", "PEST CONTROL", "90.00"),
                charge("2025-04-15", "PEST CONTROL", "90.00"),
                charge("2025-07-15", "PEST CONTROL", "90.00"),
                charge("2025-10-15", "PEST CONTROL", "90.00"));
        Subscription pest = detector.detect(quarterly).get(0);
        assertEquals(Cadence.QUARTERLY, pest.cadence());
        assertEquals(new BigDecimal("30.00"), pest.monthlyCost());

        List<Charge> yearly = List.of(
                charge("2023-11-02", "AMAZON PRIME", "139.00"),
                charge("2024-11-02", "AMAZON PRIME", "139.00"),
                charge("2025-11-02", "AMAZON PRIME", "139.00"));
        Subscription prime = detector.detect(yearly).get(0);
        assertEquals(Cadence.YEARLY, prime.cadence());
        assertEquals(new BigDecimal("139.00"), prime.yearlyCost());
        assertEquals(new BigDecimal("11.58"), prime.monthlyCost());
    }

    @Test
    void ignoresMerchantsWithFewerThanThreeCharges() {
        assertTrue(detector.detect(monthly("HULU", "9.99", 5, "2026-01", 2)).isEmpty());
    }

    @Test
    void ignoresRegularVisitsWhenTheAmountKeepsChanging() {
        List<Charge> groceries = List.of(
                charge("2026-01-04", "KROGER #123", "52.10"),
                charge("2026-02-03", "KROGER #123", "118.75"),
                charge("2026-03-05", "KROGER #123", "34.20"),
                charge("2026-04-04", "KROGER #123", "91.00"),
                charge("2026-05-04", "KROGER #123", "67.45"));
        assertTrue(detector.detect(groceries).isEmpty());
    }

    @Test
    void ignoresSameAmountAtRandomTimes() {
        List<Charge> random = List.of(
                charge("2026-01-02", "COFFEE SHOP", "4.50"),
                charge("2026-01-09", "COFFEE SHOP", "4.50"),
                charge("2026-02-21", "COFFEE SHOP", "4.50"),
                charge("2026-02-22", "COFFEE SHOP", "4.50"),
                charge("2026-04-30", "COFFEE SHOP", "4.50"));
        assertTrue(detector.detect(random).isEmpty());
    }

    @Test
    void toleratesAFewDaysOfDriftInBillingDates() {
        List<Charge> drifting = List.of(
                charge("2026-01-30", "PHONE BILL", "45.00"),
                charge("2026-03-01", "PHONE BILL", "45.00"),
                charge("2026-03-30", "PHONE BILL", "45.00"),
                charge("2026-05-01", "PHONE BILL", "45.00"));
        assertEquals(1, detector.detect(drifting).size());
    }

    @Test
    void marksASubscriptionInactiveWhenTheNextChargeNeverCame() {
        List<Charge> charges = monthly("HULU", "17.99", 5, "2026-01", 3); // Jan, Feb, Mar
        charges.addAll(monthly("NETFLIX.COM", "15.49", 3, "2026-01", 8)); // keeps going through August

        List<Subscription> found = detector.detect(charges);

        Subscription hulu = found.stream().filter(s -> s.merchant().equals("Hulu")).findFirst().orElseThrow();
        Subscription netflix = found.stream().filter(s -> s.merchant().equals("Netflix")).findFirst().orElseThrow();
        assertFalse(hulu.active());
        assertTrue(netflix.active());
    }

    @Test
    void listsActiveSubscriptionsFirstThenMostExpensive() {
        List<Charge> charges = new ArrayList<>();
        charges.addAll(monthly("CHEAP APP", "2.99", 14, "2026-01", 6));
        charges.addAll(monthly("GYM", "39.00", 1, "2026-01", 6));
        charges.addAll(monthly("OLD SERVICE", "99.00", 5, "2026-01", 3));

        List<String> order = detector.detect(charges).stream().map(Subscription::merchant).toList();

        assertEquals(List.of("Gym", "Cheap App", "Old Service"), order);
    }

    @Test
    void emptyInputGivesEmptyResult() {
        assertTrue(detector.detect(List.of()).isEmpty());
    }

    @Test
    void summaryAddsUpActiveSubscriptionsOnly() {
        List<Charge> charges = new ArrayList<>();
        charges.addAll(monthly("NETFLIX.COM", "15.49", 3, "2026-01", 4));
        charges.addAll(monthly("NETFLIX.COM", "17.99", 3, "2026-05", 4)); // price rises, runs through August
        charges.addAll(monthly("GYM", "39.00", 1, "2026-01", 8));
        charges.addAll(monthly("HULU", "17.99", 5, "2026-01", 3)); // stopped in March

        SubscriptionSummary summary = SubscriptionSummary.of(detector.detect(charges));

        assertEquals(2, summary.activeCount());
        assertEquals(new BigDecimal("56.99"), summary.monthlyTotal());
        assertEquals(new BigDecimal("683.88"), summary.yearlyTotal());
        assertEquals(1, summary.priceIncreases().size());
        assertEquals("Netflix", summary.priceIncreases().get(0).merchant());
        assertEquals(1, summary.possiblyCancelled().size());
        assertEquals("Hulu", summary.possiblyCancelled().get(0).merchant());
    }

    @Test
    void aPriceIncreaseOnlyOnTheLatestChargeIsStillASubscription() {
        List<Charge> charges = monthly("SPOTIFY", "10.99", 8, "2026-01", 5);
        charges.add(charge("2026-06-08", "SPOTIFY", "11.99"));

        Subscription spotify = detector.detect(charges).get(0);

        assertEquals(new BigDecimal("9.1"), spotify.priceChangePercent());
    }

    @Test
    void fluctuatingAmountsAreNotMistakenForOnePriceChange() {
        List<Charge> charges = List.of(
                charge("2026-01-04", "KROGER", "52.10"),
                charge("2026-02-03", "KROGER", "118.75"),
                charge("2026-03-05", "KROGER", "34.20"),
                charge("2026-04-04", "KROGER", "91.00"),
                charge("2026-05-04", "KROGER", "67.45"),
                charge("2026-06-03", "KROGER", "110.30"));
        assertTrue(detector.detect(charges).isEmpty());
    }
}

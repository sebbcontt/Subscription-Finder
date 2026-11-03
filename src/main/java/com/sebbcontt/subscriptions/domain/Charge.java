package com.sebbcontt.subscriptions.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

/** One charge: money that left the account. The amount is always positive. */
public record Charge(LocalDate date, String description, BigDecimal amount) {
}

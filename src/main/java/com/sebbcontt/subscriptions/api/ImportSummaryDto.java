package com.sebbcontt.subscriptions.api;

import java.time.Instant;

public record ImportSummaryDto(long id, String filename, Instant createdAt) {
}

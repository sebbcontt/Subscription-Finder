package com.sebbcontt.subscriptions.api;

/** What the upload endpoint returns. */
public record ImportResult(long id, String filename, int rowsRead, int charges, int subscriptionsFound) {
}

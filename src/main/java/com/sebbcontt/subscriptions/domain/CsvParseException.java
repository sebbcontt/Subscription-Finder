package com.sebbcontt.subscriptions.domain;

/** The uploaded file isn't a CSV we can read. The message is safe to show the user. */
public class CsvParseException extends RuntimeException {
    public CsvParseException(String message) {
        super(message);
    }
}

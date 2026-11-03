package com.sebbcontt.subscriptions.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Reads a bank or card export. It needs a header row with columns named
 * date, description and amount (any order, any other columns are ignored).
 *
 * If any amounts are negative, the negative ones are the charges (the usual bank export).
 * If none are negative, every positive amount is treated as a charge (the usual card export).
 */
public final class CsvParser {

    public static final int MAX_ROWS = 50_000;
    private static final DateTimeFormatter US_DATE = DateTimeFormatter.ofPattern("M/d/yyyy");

    private CsvParser() {
    }

    public static ParsedCsv parse(String csv) {
        List<List<String>> rows = readRows(csv);
        if (rows.isEmpty()) {
            throw new CsvParseException("The file is empty.");
        }

        List<String> header = rows.get(0).stream().map(h -> h.trim().toLowerCase(Locale.ROOT)).toList();
        int dateCol = header.indexOf("date");
        int descCol = header.indexOf("description");
        int amountCol = header.indexOf("amount");
        if (dateCol < 0 || descCol < 0 || amountCol < 0) {
            throw new CsvParseException("The first row must have columns named date, description and amount.");
        }
        if (rows.size() - 1 > MAX_ROWS) {
            throw new CsvParseException("Too many rows. The limit is " + MAX_ROWS + ".");
        }

        int widest = Math.max(dateCol, Math.max(descCol, amountCol));
        List<Charge> all = new ArrayList<>();
        boolean anyNegative = false;
        for (int i = 1; i < rows.size(); i++) {
            List<String> row = rows.get(i);
            int rowNumber = i + 1; // matches what a spreadsheet shows
            if (row.size() <= widest) {
                throw new CsvParseException("Row " + rowNumber + " doesn't have all the columns.");
            }
            LocalDate date = parseDate(row.get(dateCol).trim(), rowNumber);
            BigDecimal amount = parseAmount(row.get(amountCol).trim(), rowNumber);
            anyNegative |= amount.signum() < 0;
            all.add(new Charge(date, row.get(descCol).trim(), amount));
        }

        List<Charge> charges = new ArrayList<>();
        for (Charge c : all) {
            if (anyNegative && c.amount().signum() < 0) {
                charges.add(new Charge(c.date(), c.description(), c.amount().negate()));
            } else if (!anyNegative && c.amount().signum() > 0) {
                charges.add(c);
            }
        }
        return new ParsedCsv(all.size(), charges);
    }

    private static LocalDate parseDate(String text, int rowNumber) {
        try {
            return text.contains("/") ? LocalDate.parse(text, US_DATE) : LocalDate.parse(text);
        } catch (DateTimeParseException e) {
            throw new CsvParseException("Row " + rowNumber + ": can't read the date \"" + text
                    + "\". Use 2026-03-15 or 3/15/2026.");
        }
    }

    private static BigDecimal parseAmount(String text, int rowNumber) {
        String cleaned = text.replace("$", "").replace(",", "").trim();
        boolean parentheses = cleaned.startsWith("(") && cleaned.endsWith(")");
        if (parentheses) {
            cleaned = "-" + cleaned.substring(1, cleaned.length() - 1);
        }
        try {
            return new BigDecimal(cleaned);
        } catch (NumberFormatException e) {
            throw new CsvParseException("Row " + rowNumber + ": can't read the amount \"" + text + "\".");
        }
    }

    /** Splits CSV text into rows of fields, handling quoted fields with commas and doubled quotes. */
    private static List<List<String>> readRows(String csv) {
        List<List<String>> rows = new ArrayList<>();
        List<String> row = new ArrayList<>();
        StringBuilder field = new StringBuilder();
        boolean inQuotes = false;
        String text = csv.startsWith("﻿") ? csv.substring(1) : csv;

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (inQuotes) {
                if (c == '"' && i + 1 < text.length() && text.charAt(i + 1) == '"') {
                    field.append('"');
                    i++;
                } else if (c == '"') {
                    inQuotes = false;
                } else {
                    field.append(c);
                }
            } else if (c == '"') {
                inQuotes = true;
            } else if (c == ',') {
                row.add(field.toString());
                field.setLength(0);
            } else if (c == '\n' || c == '\r') {
                if (c == '\r' && i + 1 < text.length() && text.charAt(i + 1) == '\n') {
                    i++;
                }
                row.add(field.toString());
                field.setLength(0);
                addIfNotBlank(rows, row);
                row = new ArrayList<>();
            } else {
                field.append(c);
            }
        }
        if (inQuotes) {
            throw new CsvParseException("A quoted value is never closed.");
        }
        row.add(field.toString());
        addIfNotBlank(rows, row);
        return rows;
    }

    private static void addIfNotBlank(List<List<String>> rows, List<String> row) {
        boolean blank = row.stream().allMatch(f -> f.trim().isEmpty());
        if (!blank) {
            rows.add(row);
        }
    }
}

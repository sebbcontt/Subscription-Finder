package com.sebbcontt.subscriptions.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class CsvParserTest {

    @Test
    void readsNegativeAmountsAsChargesAndIgnoresDeposits() {
        String csv = """
                date,description,amount
                2026-01-03,NETFLIX.COM,-15.49
                2026-01-05,PAYROLL,2400.00
                """;
        ParsedCsv parsed = CsvParser.parse(csv);

        assertEquals(2, parsed.rowsRead());
        assertEquals(1, parsed.charges().size());
        assertEquals(new BigDecimal("15.49"), parsed.charges().get(0).amount());
        assertEquals(LocalDate.of(2026, 1, 3), parsed.charges().get(0).date());
    }

    @Test
    void treatsPositiveAmountsAsChargesWhenNothingIsNegative() {
        String csv = "date,description,amount\n2026-01-03,SPOTIFY,11.99\n2026-02-03,SPOTIFY,11.99\n";
        assertEquals(2, CsvParser.parse(csv).charges().size());
    }

    @Test
    void columnsCanBeInAnyOrderWithExtrasAndCaseDoesNotMatter() {
        String csv = "Balance,Amount,Date,Category,Description\n100,-9.99,2026-03-01,Fun,HULU\n";
        ParsedCsv parsed = CsvParser.parse(csv);
        assertEquals("HULU", parsed.charges().get(0).description());
        assertEquals(new BigDecimal("9.99"), parsed.charges().get(0).amount());
    }

    @Test
    void handlesQuotedFieldsWithCommasAndDollarSigns() {
        String csv = "date,description,amount\n2026-01-03,\"SQ *COFFEE, OAKLAND CA\",\"-$1,234.50\"\n";
        Charge charge = CsvParser.parse(csv).charges().get(0);
        assertEquals("SQ *COFFEE, OAKLAND CA", charge.description());
        assertEquals(new BigDecimal("1234.50"), charge.amount());
    }

    @Test
    void readsUsDatesAndParenthesesAsNegative() {
        String csv = "date,description,amount\n3/5/2026,GYM,(39.00)\n";
        Charge charge = CsvParser.parse(csv).charges().get(0);
        assertEquals(LocalDate.of(2026, 3, 5), charge.date());
        assertEquals(new BigDecimal("39.00"), charge.amount());
    }

    @Test
    void handlesWindowsLineEndingsBlankLinesAndByteOrderMark() {
        String csv = "﻿date,description,amount\r\n\r\n2026-01-03,HULU,-9.99\r\n";
        assertEquals(1, CsvParser.parse(csv).charges().size());
    }

    @Test
    void emptyFileIsRejected() {
        assertThrows(CsvParseException.class, () -> CsvParser.parse(""));
    }

    @Test
    void missingColumnsAreRejectedWithAClearMessage() {
        CsvParseException e = assertThrows(CsvParseException.class,
                () -> CsvParser.parse("when,what,howmuch\n2026-01-01,X,1\n"));
        assertTrue(e.getMessage().contains("date, description and amount"));
    }

    @Test
    void badDateNamesTheRow() {
        CsvParseException e = assertThrows(CsvParseException.class,
                () -> CsvParser.parse("date,description,amount\n2026-01-03,A,-1\nnot-a-date,B,-2\n"));
        assertTrue(e.getMessage().startsWith("Row 3"));
    }

    @Test
    void badAmountNamesTheRow() {
        CsvParseException e = assertThrows(CsvParseException.class,
                () -> CsvParser.parse("date,description,amount\n2026-01-03,A,abc\n"));
        assertTrue(e.getMessage().startsWith("Row 2"));
    }

    @Test
    void shortRowIsRejected() {
        assertThrows(CsvParseException.class, () -> CsvParser.parse("date,description,amount\n2026-01-03,A\n"));
    }

    @Test
    void unclosedQuoteIsRejected() {
        assertThrows(CsvParseException.class,
                () -> CsvParser.parse("date,description,amount\n2026-01-03,\"A,-1\n"));
    }
}

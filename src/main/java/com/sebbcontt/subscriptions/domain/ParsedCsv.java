package com.sebbcontt.subscriptions.domain;

import java.util.List;

/** @param rowsRead data rows in the file, including deposits we ignore */
public record ParsedCsv(int rowsRead, List<Charge> charges) {
}

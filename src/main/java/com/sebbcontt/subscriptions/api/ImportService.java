package com.sebbcontt.subscriptions.api;

import com.sebbcontt.subscriptions.domain.Charge;
import com.sebbcontt.subscriptions.domain.CsvParser;
import com.sebbcontt.subscriptions.domain.ParsedCsv;
import com.sebbcontt.subscriptions.domain.RecurringDetector;
import com.sebbcontt.subscriptions.domain.Subscription;
import com.sebbcontt.subscriptions.persistence.ChargeEntity;
import com.sebbcontt.subscriptions.persistence.ChargeRepository;
import com.sebbcontt.subscriptions.persistence.ImportEntity;
import com.sebbcontt.subscriptions.persistence.ImportRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ImportService {

    private final ImportRepository imports;
    private final ChargeRepository chargeRepository;
    private final RecurringDetector detector = new RecurringDetector();

    public ImportService(ImportRepository imports, ChargeRepository chargeRepository) {
        this.imports = imports;
        this.chargeRepository = chargeRepository;
    }

    @Transactional
    public ImportResult createImport(String filename, String csvText) {
        ParsedCsv parsed = CsvParser.parse(csvText);

        ImportEntity saved = imports.save(new ImportEntity(filename));
        List<ChargeEntity> rows = parsed.charges().stream()
                .map(charge -> new ChargeEntity(saved.getId(), charge))
                .toList();
        chargeRepository.saveAll(rows);

        int found = detector.detect(parsed.charges()).size();
        return new ImportResult(saved.getId(), filename, parsed.rowsRead(), parsed.charges().size(), found);
    }

    @Transactional(readOnly = true)
    public List<ImportSummaryDto> listImports() {
        return imports.findAll().stream()
                .map(i -> new ImportSummaryDto(i.getId(), i.getFilename(), i.getCreatedAt()))
                .toList();
    }

    /** Empty when there is no import with that id. */
    @Transactional(readOnly = true)
    public Optional<List<Subscription>> subscriptionsFor(long importId) {
        if (!imports.existsById(importId)) {
            return Optional.empty();
        }
        List<Charge> charges = chargeRepository.findByImportId(importId).stream()
                .map(ChargeEntity::toCharge)
                .toList();
        return Optional.of(detector.detect(charges));
    }
}

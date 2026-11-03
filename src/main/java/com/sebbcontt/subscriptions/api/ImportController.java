package com.sebbcontt.subscriptions.api;

import com.sebbcontt.subscriptions.domain.Subscription;
import com.sebbcontt.subscriptions.domain.SubscriptionSummary;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/imports")
public class ImportController {

    private final ImportService service;

    public ImportController(ImportService service) {
        this.service = service;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ImportResult> upload(@RequestParam("file") MultipartFile file) throws IOException {
        String name = file.getOriginalFilename() == null ? "upload.csv" : file.getOriginalFilename();
        if (file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The file is empty.");
        }
        if (!name.toLowerCase(Locale.ROOT).endsWith(".csv")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Please upload a .csv file.");
        }

        ImportResult result = service.createImport(name, new String(file.getBytes(), StandardCharsets.UTF_8));
        return ResponseEntity.created(URI.create("/api/imports/" + result.id())).body(result);
    }

    @GetMapping
    public List<ImportSummaryDto> list() {
        return service.listImports();
    }

    @GetMapping("/{id}/subscriptions")
    public List<Subscription> subscriptions(@PathVariable long id) {
        return service.subscriptionsFor(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No import with id " + id));
    }

    @GetMapping("/{id}/summary")
    public SubscriptionSummary summary(@PathVariable long id) {
        return SubscriptionSummary.of(subscriptions(id));
    }
}

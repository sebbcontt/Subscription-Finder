package com.sebbcontt.subscriptions.persistence;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChargeRepository extends JpaRepository<ChargeEntity, Long> {

    List<ChargeEntity> findByImportId(Long importId);
}

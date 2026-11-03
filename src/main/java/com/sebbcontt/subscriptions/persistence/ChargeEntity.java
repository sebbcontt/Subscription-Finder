package com.sebbcontt.subscriptions.persistence;

import com.sebbcontt.subscriptions.domain.Charge;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "charges")
public class ChargeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "import_id", nullable = false)
    private Long importId;

    @Column(name = "charge_date", nullable = false)
    private LocalDate chargeDate;

    @Column(nullable = false, length = 500)
    private String description;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    protected ChargeEntity() {
    }

    public ChargeEntity(Long importId, Charge charge) {
        this.importId = importId;
        this.chargeDate = charge.date();
        // The column holds 500 characters, so cut anything longer instead of failing the whole upload.
        this.description = charge.description().length() > 500
                ? charge.description().substring(0, 500)
                : charge.description();
        this.amount = charge.amount();
    }

    public Charge toCharge() {
        return new Charge(chargeDate, description, amount);
    }
}

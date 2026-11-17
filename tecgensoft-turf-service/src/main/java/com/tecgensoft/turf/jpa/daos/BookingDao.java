package com.tecgensoft.turf.jpa.daos;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "bookings")
public class BookingDao {
  @Id private UUID id;
  private Instant createdAt;
  private Instant updatedAt;
  private UUID organizationId;

  private UUID courtId;
  private UUID reservationId;
  private java.time.LocalDate localDate;
  private int startHour;
  private int durationHours;
  private String customerName;
  private String customerPhone;
  private String customerEmail;
  private BigDecimal total;
  private BigDecimal requiredAdvance;
  private String status;
  private String source;
  private String notes;
  private String cancellationReason;
  private Instant cancelledAt;
  private UUID cancelledBy;
  private String submittedReference;

  @PrePersist
  void create() {
    if (id == null) id = com.tecgensoft.turf.common.Ids.next();
    createdAt = Instant.now();
    updatedAt = createdAt;
  }

  @PreUpdate
  void update() {
    updatedAt = Instant.now();
  }
}

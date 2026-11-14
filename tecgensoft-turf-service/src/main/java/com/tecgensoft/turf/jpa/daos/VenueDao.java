package com.tecgensoft.turf.jpa.daos;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "venues")
public class VenueDao {
  @Id private UUID id;
  private Instant createdAt;
  private Instant updatedAt;
  private UUID organizationId;
  private UUID branchId;
  private String name;
  private String phone;
  private String address;
  private String timezone;
  private int openingHour;
  private int closingHour;
  private int peakStartHour;
  private int advancePercentage;
  private int holdMinutes;
  private int maxDurationHours;
  private int bookingWindowDays;
  private int cancellationCutoffHours;

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

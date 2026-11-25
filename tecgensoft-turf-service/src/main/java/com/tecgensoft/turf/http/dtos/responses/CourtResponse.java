package com.tecgensoft.turf.http.dtos.responses;

import com.tecgensoft.turf.service.domain.Court;
import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;

/** Court read model returned by the court API. Mirrors the domain court exactly. */
public record CourtResponse(
    UUID id,
    UUID organizationId,
    String name,
    String sport,
    String surface,
    boolean indoor,
    String size,
    String imageUrl,
    BigDecimal regularPrice,
    BigDecimal peakPrice,
    int players,
    Set<String> features,
    String description,
    boolean active) {

  public static CourtResponse from(Court court) {
    return new CourtResponse(
        court.id(),
        court.organizationId(),
        court.name(),
        court.sport(),
        court.surface(),
        court.indoor(),
        court.size(),
        court.imageUrl(),
        court.regularPrice(),
        court.peakPrice(),
        court.players(),
        court.features(),
        court.description(),
        court.active());
  }
}
